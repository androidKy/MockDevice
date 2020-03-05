/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.gun.local.internal;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.provider.Settings;
import android.util.Base64;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gun.local.LocalVPNService;
import com.gun.local.GunLib;
import com.gun.local.handler.CommonManager;
import com.gun.local.internal.Packet.TCPHeader;
import com.gun.local.internal.TCB.TCBStatus;
import com.gun.local.tool.Logs;
import com.gun.local.tool.PrefUtils;
import com.gun.local.tool.StupidUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPOutput implements Runnable, CommonManager.ISocketListener {
    private static final String TAG = "TCPOutput";

    private LocalVPNService vpnService;
    private LinkedBlockingQueue<Packet> deviceToNetworkTCPQueue;
    private LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;

    private Random random = new Random();
    private SocketChannel sGlobaloutputChannel;

    public TCPOutput(LinkedBlockingQueue<Packet> deviceToNetworkTCPQueue, LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue,
                     Selector selector, LocalVPNService vpnService) {
        this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
        this.vpnService = vpnService;
        CommonManager.getInstance().addISocketListener(this);
    }

    @Override
    public void run() {
        Logs.i(TAG, "Started");
        try {
            if (SdkConfig.ISPROXYMODE) {
                proxyModeOpen(GunLib.getReConnectB());
            }

            Thread currentThread = Thread.currentThread();
            while (true) {
                Packet currentPacket;
                // TODO: Block when not connected
                do {
                    currentPacket = deviceToNetworkTCPQueue.take();
                    if (currentPacket != null)
                        break;
//                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());

                if (currentThread.isInterrupted())
                    break;

                ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                currentPacket.backingBuffer = null;
                ByteBuffer responseBuffer = ByteBufferPool.acquire();

                InetAddress destinationAddress = currentPacket.ip4Header.destinationAddress;

                TCPHeader tcpHeader = currentPacket.tcpHeader;
                int destinationPort = tcpHeader.destinationPort;
                int sourcePort = tcpHeader.sourcePort;

                String ipAndPort = destinationAddress.getHostAddress() + ":" +
                        destinationPort + ":" + sourcePort;
                TCB tcb = TCB.getTCB(ipAndPort);

                synchronized (networkToDeviceQueue) {
                    if (tcb == null) {
                        initializeConnection(ipAndPort, destinationAddress, destinationPort,
                                currentPacket, tcpHeader, responseBuffer);
                    } else if (tcpHeader.isSYN()) {
                        processDuplicateSYN(tcb, tcpHeader, responseBuffer);
                    } else if (tcpHeader.isRST()) {
                        Logs.d(TAG, "isRST " + tcb.ipAndPort);
                        closeCleanly(tcb, responseBuffer);
                    } else if (tcpHeader.isFIN()) {
                        processFIN(tcb, tcpHeader, responseBuffer);
                    } else if (tcpHeader.isACK()) {
                        processACK(tcb, tcpHeader, payloadBuffer, responseBuffer);
                    }
                    // XXX: cleanup later
                    if (responseBuffer.position() == 0)
                        ByteBufferPool.release(responseBuffer);
                    ByteBufferPool.release(payloadBuffer);
                }
            }
        } catch (InterruptedException e) {
            Logs.i(TAG, "Stopping");
        } catch (IOException e) {
            Logs.e(TAG, "Stopping" + e.toString(), e);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            TCB.closeAll();
        }
    }

    private void initializeConnection(String ipAndPort, InetAddress destinationAddress, int destinationPort,
                                      Packet currentPacket, TCPHeader tcpHeader, ByteBuffer responseBuffer)
            throws IOException {
        currentPacket.swapSourceAndDestination();
        if (tcpHeader.isSYN()) {
            SocketChannel outputChannel;
            if (SdkConfig.ISPROXYMODE) {
                outputChannel = sGlobaloutputChannel;
            } else {
                outputChannel = SocketChannel.open();
                outputChannel.configureBlocking(false);
                vpnService.protect(outputChannel.socket());
            }


            TCB tcb = new TCB(ipAndPort, random.nextInt(Short.MAX_VALUE + 1), tcpHeader.sequenceNumber, tcpHeader.sequenceNumber + 1,
                    tcpHeader.acknowledgementNumber, outputChannel, currentPacket);
            TCB.putTCB(ipAndPort, tcb);
            try {
                if (SdkConfig.ISPROXYMODE) {
                    if (!outputChannel.isConnected()) {
                        try {
                            openGlobalChannel(outputChannel, false);
                        } catch (Throwable e) {
                            Logs.e(TAG, "ISPROXYMODE Connection error:" + e.toString());
                            proxyModeClose();
                            proxyModeOpen(GunLib.getExceptionReConnectB());
                            TCB.closeTCB(tcb);
                            return;
                        }
                    }
                } else {
                    outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                }

                if (outputChannel.finishConnect()) {
                    tcb.status = TCBStatus.SYN_RECEIVED;
                    // TODO: Set MSS for receiving larger packets from the device
                    currentPacket.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                            tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                    tcb.mySequenceNum++; // SYN counts as a byte

                    Logs.d(TAG, "new TCB SYN_RECEIVED " + tcb.ipAndPort);
                } else {
                    tcb.status = TCBStatus.SYN_SENT;
                    selector.wakeup();
                    tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_CONNECT, tcb);
                    Logs.d(TAG, "new TCB SYN_SENT " + tcb.ipAndPort);

                    return;
                }
            } catch (IOException e) {
                Logs.e(TAG, "Connection error: " + ipAndPort, e);
                currentPacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                TCB.closeTCB(tcb);
                CommonManager.getInstance().addSocketException(e);
                Logs.d(TAG, "new TCB IOException ");
            }

        } else {
            if (SdkConfig.ISPROXYMODE) {
                return;
            }
            synchronized (networkToDeviceQueue) {
                currentPacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.RST,
                        0, tcpHeader.sequenceNumber + 1, 0);
                Logs.d(TAG, "new TCB RST " + tcpHeader.sourcePort);
            }
        }
        networkToDeviceQueue.offer(responseBuffer);
    }

    private synchronized void openGlobalChannel(SocketChannel outputChannel, boolean isReConnect) {
        try {
            Logs.i(TAG, "openGlobalChannel,isReConnect:" + isReConnect);
            vpnService.protect(outputChannel.socket());
            outputChannel.configureBlocking(true);
            String hostName = PrefUtils.getCommonSP(GunLib.getContext()).getString(PrefUtils.PrefKeys.PROXY_ADDRESS, "");
            int port = PrefUtils.getCommonSP(GunLib.getContext()).getInt(PrefUtils.PrefKeys.PROXY_PORT, 0);
            Logs.i("hostName:" + hostName + ",port:" + port);
            Logs.i("connect server");
            Logs.i("connect server : " + outputChannel.isOpen());

            boolean result = outputChannel.connect(new InetSocketAddress(hostName, port));
            Logs.i("connect result:" + result);
            if(result){
                ruleConfigure(outputChannel);
            }
            //ruleConfigure(outputChannel, isReConnect);
            outputChannel.configureBlocking(false);
            LocalBroadcastManager.getInstance(GunLib.getContext()).sendBroadcast(new Intent(LocalVPNService.ACTION_CONNECTED).putExtra("connected", result));
            //发送心跳
           /* CommonManager.getInstance().restartHeart(outputChannel);
            CommonManager.getInstance().sendHeart(false);*/
        } catch (IOException e) {
            try {
                outputChannel.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            LocalBroadcastManager.getInstance(GunLib.getContext()).sendBroadcast(new Intent(LocalVPNService.ACTION_CONNECTED).putExtra("connected", false));
            e.printStackTrace();
        }
    }

    public void sendClosePacket() {
        ByteBuffer receiveBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE);
        receiveBuffer.put(Protocol.CLOSE_A_B);
        Logs.i(TAG, "sendClosePacket");
        receiveBuffer.position(0);
        receiveBuffer.limit(Packet.HEADER_SIZE);
        try {
            if (!sGlobaloutputChannel.isOpen()) return;
            sGlobaloutputChannel.write(receiveBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            Logs.i(TAG, "sendClosePacket exception:" + e.getMessage());
        }
    }

    public void close() throws IOException {
        sGlobaloutputChannel.close();
    }

    private void processDuplicateSYN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
        synchronized (tcb) {
            if (tcb.status == TCBStatus.SYN_SENT) {
                tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
                Logs.d(TAG, "isSYN SYN_SENT " + tcb.ipAndPort);
                return;
            }
        }
        Logs.d(TAG, "syn go RST " + tcb.ipAndPort);
        sendRST(tcb, 1, responseBuffer);
    }

    private void processFIN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer) {
        synchronized (tcb) {
            Packet referencePacket = tcb.referencePacket;
            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;

            if (tcb.waitingForNetworkData) {
                tcb.status = TCBStatus.CLOSE_WAIT;
                referencePacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK,
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                Logs.d(TAG, "isFIN CLOSE_WAIT " + tcb.ipAndPort);
            } else {
                tcb.status = TCBStatus.LAST_ACK;
                referencePacket.updateTCPBuffer(responseBuffer, (byte) (TCPHeader.FIN | TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
                Logs.d(TAG, "isFIN LAST_ACK " + tcb.ipAndPort);
            }
        }
        networkToDeviceQueue.offer(responseBuffer);
    }

    private void processACK(TCB tcb, TCPHeader tcpHeader, ByteBuffer payloadBuffer, ByteBuffer responseBuffer) throws IOException {
        int payloadSize = payloadBuffer.limit() - payloadBuffer.position();

        synchronized (tcb) {
            SocketChannel outputChannel = tcb.channel;
            if (SdkConfig.ISPROXYMODE) {
                outputChannel = sGlobaloutputChannel;
            }

            if (tcb.status == TCBStatus.SYN_RECEIVED) {
                tcb.status = TCBStatus.ESTABLISHED;

                selector.wakeup();
                tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_READ, tcb);
                tcb.waitingForNetworkData = true;

                Logs.d(TAG, "isACK ESTABLISHED" + tcb.ipAndPort);
            } else if (tcb.status == TCBStatus.LAST_ACK) {
                closeCleanly(tcb, responseBuffer);
                Logs.d(TAG, "isACK LAST_ACK" + tcb.ipAndPort);
                return;
            }

            if (payloadSize == 0) {
                Logs.d(TAG, "isACK Empty ACK, ignore");
                return; // Empty ACK, ignore
            }

            if (!tcb.waitingForNetworkData) {
                selector.wakeup();
                tcb.selectionKey.interestOps(SelectionKey.OP_READ);
                tcb.waitingForNetworkData = true;
                Logs.d(TAG, "isACK, !tcb.waitingForNetworkData");
            }

            // Forward to remote server
            try {
                while (payloadBuffer.hasRemaining()) {
                    if (payloadBuffer.limit() == 40) {
                        break;
                    }
                    Logs.d(TAG, payloadBuffer.limit() + " isACK write " + tcb.referencePacket.ip4Header.sourceAddress + " " + tcb.referencePacket.tcpHeader.sourcePort
                            + " " + tcb.referencePacket.ip4Header.destinationAddress + " " + tcb.referencePacket.tcpHeader.destinationPort);
                    outputChannel.write(payloadBuffer);
                }
            } catch (IOException e) {
                Logs.e(TAG, "Network write error: " + tcb.ipAndPort, e);
                if (SdkConfig.ISPROXYMODE) {
//                    TCB.closeTCB(tcb);
                    TCB.closeAll();
                    proxyModeClose();
                    proxyModeOpen(GunLib.getExceptionReConnectB());
                } else {
                    sendRST(tcb, payloadSize, responseBuffer);
                }
                return;
            }

            // TODO: We don't expect out-of-order packets, but verify
            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + payloadSize;

            //代理模式减去ip&tcp头部的长度
            if (SdkConfig.ISPROXYMODE) {
                tcb.myAcknowledgementNum -= 40;
            }

            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;
            Packet referencePacket = tcb.referencePacket;
            referencePacket.updateTCPBuffer(responseBuffer, (byte) TCPHeader.ACK, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
        }
        networkToDeviceQueue.offer(responseBuffer);
    }

    private void sendRST(TCB tcb, int prevPayloadSize, ByteBuffer buffer) {
        Logs.d(TAG, "sendRST isRST " + tcb.ipAndPort);
        tcb.referencePacket.updateTCPBuffer(buffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum + prevPayloadSize, 0);
        networkToDeviceQueue.offer(buffer);
//        TCB.closeTCB(tcb);
    }

    private void closeCleanly(TCB tcb, ByteBuffer buffer) {
        if (SdkConfig.ISPROXYMODE) {
            try {
                Logs.d(TAG, "closeCleanly");
                //目的地址默认返回虚拟网卡，代理模式中需重新翻转
                tcb.referencePacket.swapSourceAndDestination();
                tcb.referencePacket.updateTCPBuffer(buffer, (byte) TCPHeader.RST, 0, 0, 0);
                buffer.position(0);
                buffer.limit(Packet.HEADER_SIZE);
//                tcb.channel.write(buffer);
                sGlobaloutputChannel.write(buffer);
            } catch (Exception e) {

            }
        }
        ByteBufferPool.release(buffer);
        TCB.closeTCB(tcb);
    }

    private void proxyModeOpen(boolean isReConnect) {
        try {
            if (SdkConfig.ISPROXYMODE) {
                sGlobaloutputChannel = SocketChannel.open();
                openGlobalChannel(sGlobaloutputChannel, isReConnect);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Logs.d(TAG, "proxyModeOpen Exception " + e.getMessage());
        }
    }

    private void proxyModeClose() {
        try {
            if (SdkConfig.ISPROXYMODE) {
                sGlobaloutputChannel.close();
            }
        } catch (Exception e) {
            Logs.d(TAG, "proxyModeClose Exception " + e.getMessage());
        }
    }

    private void ruleConfigure(SocketChannel socketChannel){
       /* String userInfo = m_Config.UserName.replaceAll("\\(", "") + ":" + m_Config.Password.replaceAll("\\)", "");
        String authData = Base64.encodeToString(userInfo.getBytes(), Base64.DEFAULT);
        @SuppressLint("DefaultLocale") String request =
                String.format("CONNECT %s:%d HTTP/1.0\r\nProxy-Authorization: Basic %s\r\n%s\r\nProxy-Connection: keep-alive\r\nUser-Agent: %s\r\nX-App-Install-ID: %s\r\n\r\n",
                        m_DestAddress.getHostName(),
                        m_DestAddress.getPort(),
                        authData,
                        userInfo,
                        ProxyConfig.Instance.getUserAgent(),
                        ProxyConfig.AppInstallID);*/
    }

    private void ruleConfigure(SocketChannel socketChannel, boolean isReConnect) {
        try {
            //写协议规则以及android
            String androidId = Settings.Secure.getString(GunLib.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            char[] chars = androidId.toCharArray();

            Logs.i(TAG, "chars length:" + chars.length + ",androidId:" + androidId);

            ByteBuffer ruleCommand = ByteBuffer.allocate(Packet.HEADER_SIZE);
            ruleCommand.put(Protocol.REGISTER_A);
            ruleCommand.put(StupidUtil.intToByte((chars.length > 19 ? 19 : chars.length)));
            ruleCommand.put(StupidUtil.intToByte((isReConnect ? 1 : 0)));
            for (int i = 0; i < chars.length && i <= 19; i++) {
                ruleCommand.putChar(chars[i]);
            }
            ruleCommand.position(0);
            ruleCommand.limit(Packet.HEADER_SIZE);

            socketChannel.write(ruleCommand);

            //写appkey及country
            ByteBuffer otherBuffer = ByteBuffer.allocate(200);

            String appkey = GunLib.getAppkey();
            int appkeyLength = appkey.length();
            char[] appkeys = appkey.toCharArray();
            Logs.i(TAG, "appkey chars length:" + appkeys.length + ",appkey:" + appkey);
            otherBuffer.put(StupidUtil.intToByte(appkeyLength));
            Logs.i(TAG, "start for loop1");
            for (int i = 0; i < appkeyLength; i++) {
                otherBuffer.putChar(appkeys[i]);
            }

            String country = GunLib.getCountry();
            int countryLength = country.length();
            char[] countrys = country.toCharArray();
            Logs.i(TAG, "country chars length:" + countrys.length + ",country:" + country);
            otherBuffer.put(StupidUtil.intToByte(countryLength));
            Logs.i(TAG, "start for loop1");
            for (int i = 0; i < countryLength; i++) {
                otherBuffer.putChar(countrys[i]);
            }

            otherBuffer.position(0);
            otherBuffer.limit(200);
            socketChannel.write(otherBuffer);

            Logs.d(TAG, "ruleConfigure write ");
        } catch (Exception e) {
            e.printStackTrace();
            CommonManager.getInstance().addSocketException(e);
            Logs.d(TAG, "proxyModeClose Exception " + e.getMessage());
        }
    }

    @Override
    public void reconnect() {
        Logs.i(TAG, "reconnect");
        proxyModeClose();
        proxyModeOpen(GunLib.getExceptionReConnectB());
    }
}
