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
import android.util.Base64;

import com.gun.local.handler.CommonManager;
import com.gun.local.internal.TCB.TCBStatus;
import com.gun.local.tool.Logs;
import com.gun.local.tool.StupidUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class TCPInput implements Runnable {
    private static final String TAG = "TCPInput";
    private static final int HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE;

    private LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue;
    private Selector selector;
    private PendingBuffer mPendingBufferMap;

    public TCPInput(LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue, Selector selector) {
        this.networkToDeviceQueue = networkToDeviceQueue;
        this.selector = selector;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                if (!selector.isOpen()) {
                    Logs.d(TAG, "selector.is closed");
                    return;
                }
                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();
                while (keyIterator.hasNext() && !Thread.interrupted()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        synchronized (networkToDeviceQueue) {
                            try {
                                if (key.isConnectable()) {
                                    processConnect(key, keyIterator);
                                } else if (key.isReadable()){
                                    processInput(key, keyIterator); //todo 接收代理服务器返回的数据
                                    /*if (SdkConfig.ISPROXYMODE) {
                                        processInputProxyMode(key, keyIterator);
                                    } else {
                                        processInput(key, keyIterator);
                                    }*/
                                }
                            } catch (CancelledKeyException e) {
                                key.cancel();
                            }
                        }
                    }
                }
            }
            Logs.d(TAG, "Thread.interrupted()");
        } catch (InterruptedException e) {
            Logs.i(TAG, "Stopping");
        } catch (IOException e) {
            Logs.w(TAG, e.toString(), e);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void processConnect(SelectionKey key, Iterator<SelectionKey> keyIterator) {
        TCB tcb = (TCB) key.attachment();
        Packet referencePacket = tcb.referencePacket;
        try {
            if (tcb.channel.finishConnect()) {
                Logs.i("与代理服务器连接成功");

                keyIterator.remove();
                tcb.status = TCBStatus.SYN_RECEIVED;
                ByteBuffer responseBuffer = ByteBufferPool.acquire();

                referencePacket.updateTCPBuffer(responseBuffer, (byte) (Packet.TCPHeader.SYN | Packet.TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                networkToDeviceQueue.offer(responseBuffer);
                Logs.d(TAG, "processConnect SYN_RECEIVED " + tcb.ipAndPort);

                tcb.mySequenceNum++; // SYN counts as a byte
                key.interestOps(SelectionKey.OP_READ);
            }
        } catch (IOException e) {
            Logs.e(TAG, "Connection error: " + tcb.ipAndPort, e);
            ByteBuffer responseBuffer = ByteBufferPool.acquire();
            referencePacket.updateTCPBuffer(responseBuffer, (byte) Packet.TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
            networkToDeviceQueue.offer(responseBuffer);
            TCB.closeTCB(tcb);
        }
    }

    private void processInput(SelectionKey key, Iterator<SelectionKey> keyIterator) {
        Logs.d(TAG, "processInput");
        keyIterator.remove();
        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
        // Leave space for the header
        receiveBuffer.position(HEADER_SIZE);

        TCB tcb = (TCB) key.attachment();
        synchronized (tcb) {
            Packet referencePacket = tcb.referencePacket;
            SocketChannel inputChannel = (SocketChannel) key.channel();
            int readBytes;
            try {
                readBytes = inputChannel.read(receiveBuffer);
            } catch (IOException e) {
                Logs.e(TAG, "Network read error: " + tcb.ipAndPort, e);
                referencePacket.updateTCPBuffer(receiveBuffer, (byte) Packet.TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                networkToDeviceQueue.offer(receiveBuffer);
                TCB.closeTCB(tcb);
                return;
            }

            if (readBytes == -1) {
                // End of stream, stop waiting until we push more data
                key.interestOps(0);
                tcb.waitingForNetworkData = false;

                if (tcb.status != TCBStatus.CLOSE_WAIT) {
                    ByteBufferPool.release(receiveBuffer);
                    return;
                }

                tcb.status = TCBStatus.LAST_ACK;
                referencePacket.updateTCPBuffer(receiveBuffer, (byte) Packet.TCPHeader.FIN, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
                Logs.d(TAG, "processInput LAST_ACK " + tcb.ipAndPort);
            } else {
                // XXX: We should ideally be splitting segments by MTU/MSS, but this seems to work without
                referencePacket.updateTCPBuffer(receiveBuffer, (byte) (Packet.TCPHeader.PSH | Packet.TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, readBytes);
                tcb.mySequenceNum += readBytes; // Next sequence number
                receiveBuffer.position(HEADER_SIZE + readBytes);
                Logs.d(TAG, "processInput " + tcb.status + " " + tcb.ipAndPort);
            }
            networkToDeviceQueue.offer(receiveBuffer);
        }
    }

    private void processInputProxyMode(SelectionKey key, Iterator<SelectionKey> keyIterator) {
        Logs.d(TAG, "processInputProxyMode");
        keyIterator.remove();

        SocketChannel inputChannel = (SocketChannel) key.channel();
        try {
            ByteBuffer headByteBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE);
            int len = 0;
            do {
                len = inputChannel.read(headByteBuffer);

                if (len == -1) {
                    readErrorAndSendRST(key);
                    return;
                }
//                Logs.d(TAG, "head len: " + headByteBuffer.position()+" "+headByteBuffer.limit());
            } while (headByteBuffer.position() != headByteBuffer.limit());

            headByteBuffer.flip();
            Logs.i(TAG,"headByteBuffer:position:"+headByteBuffer.position());
            byte head = headByteBuffer.get();
            Logs.i(TAG,"head:"+head);
            if (head == Protocol.HEART_RESPONSE_A) {
                /**收到服务器回复的心跳*/
                int extraValue = headByteBuffer.getInt();
                boolean isException = headByteBuffer.get() == StupidUtil.BYTE_1;
                Logs.i(TAG, "收到服务器回复A心跳:" + extraValue + ",isException:" + isException);
                CommonManager.getInstance().onHeartReceived(System.currentTimeMillis(), extraValue, isException);
                return;
            }
            if (head == Protocol.PAIR_RESULT) {
                /**收到服务器回复配对信息*/
                byte result = headByteBuffer.get();
                String resultMsg = null;
                if (result == StupidUtil.BYTE_1) {
                    resultMsg = "配对结果：配对成功";
                } else if (result == StupidUtil.BYTE_2) {
                    resultMsg = "配对结果：配对失败，等待配对中";
                } else {
                    resultMsg = "配对结果：配对result byte解析失败：" + result;
                }
                Logs.i(TAG, resultMsg);
                return;
            }
            headByteBuffer.position(0);
            Packet packetInfo = new Packet(headByteBuffer);
            if (packetInfo.ip4Header.version != 4 || packetInfo.ip4Header.headerLength != 20) {
                Logs.e(TAG, "Packet head wrong,ip4Header.version:" + packetInfo.ip4Header.version + ",ip4Header.headerLength:" + packetInfo.ip4Header.headerLength);
                return;
            }
            Logs.d(TAG, "read from server totalLength: " + packetInfo.ip4Header.totalLength);
            if (packetInfo.ip4Header.totalLength > Packet.HEADER_SIZE) {
                ByteBuffer bufferByteBuffer = ByteBuffer.allocate(packetInfo.ip4Header.totalLength);
                bufferByteBuffer.position(Packet.HEADER_SIZE);

                int totalRead = Packet.HEADER_SIZE;
                do {
                    len = inputChannel.read(bufferByteBuffer);
                    totalRead += len;
                    if (len == -1) {
                        readErrorAndSendRST(key);
                        return;
                    }
                } while (totalRead < packetInfo.ip4Header.totalLength);
                Logs.d(TAG, "read while end: " + totalRead + " " + packetInfo.ip4Header.totalLength);

                String ipAndPort = packetInfo.ip4Header.destinationAddress.getHostAddress() + ":" +
                        packetInfo.tcpHeader.destinationPort + ":" + packetInfo.tcpHeader.sourcePort;
                TCB tcb = TCB.getTCB(ipAndPort);
                if (tcb == null) {
                    Logs.d(TAG, "tcb==null, already closed:" + ipAndPort);
                    return;
                }
                Packet referencePacket = tcb.referencePacket;
                // XXX: We should ideally be splitting segments by MTU/MSS, but this seems to work without
                referencePacket.updateTCPBuffer(bufferByteBuffer, (byte) (Packet.TCPHeader.PSH | Packet.TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, totalRead - Packet.HEADER_SIZE);
                tcb.mySequenceNum += (totalRead - Packet.HEADER_SIZE); // Next sequence number
                bufferByteBuffer.position(bufferByteBuffer.limit());
                Logs.d(TAG, "processInputProxyMode " + tcb.status + " " + tcb.ipAndPort);
                networkToDeviceQueue.offer(bufferByteBuffer);
            }

        } catch (Exception e) {
            CommonManager.getInstance().addSocketException(e);
            e.printStackTrace();
            Logs.e(TAG, "Network read error: " + e.toString());
            return;
        }
    }

    private void readErrorAndSendRST(SelectionKey key) {
        TCB tcb = (TCB) key.attachment();
        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
        Packet referencePacket = tcb.referencePacket;
        // End of stream, stop waiting until we push more data
        key.interestOps(0);
        tcb.waitingForNetworkData = false;

        if (tcb.status != TCBStatus.CLOSE_WAIT) {
            ByteBufferPool.release(receiveBuffer);
            return;
        }

        tcb.status = TCBStatus.LAST_ACK;
        referencePacket.updateTCPBuffer(receiveBuffer, (byte) Packet.TCPHeader.FIN, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
        tcb.mySequenceNum++; // FIN counts as a byte
        Logs.d(TAG, "processInput LAST_ACK " + tcb.ipAndPort);
    }

//    private Packet getPacketInfo(byte[] buffer){
//        Packet packet = null;
//        try{
//            packet = new Packet(ByteBuffer.wrap(buffer));
//        } catch (Exception e){
//            Logs.d(TAG, "getPacketInfo error "+e.toString());
//        }
//        return packet;
//    }
//
//    private void subPackge(byte[] receiveBuffer, int offset, int lenRemain) throws Exception {
//        int headerLength;
//        int ipDataLen;
//        String ipAndPort;
//        if(mPendingBufferMap == null){
//            Packet packet = new Packet(ByteBuffer.wrap(receiveBuffer, offset, lenRemain));
//            int version = packet.ip4Header.version;
//            headerLength = packet.ip4Header.headerLength;
//            ipDataLen = packet.ip4Header.totalLength;
//            Logs.d(TAG, "version hl " + version + " " + headerLength + " "
//                    + offset + " " + " " + ipDataLen + " " + lenRemain);
//            if (version != 4 && headerLength != 5) {//5个整形20字节
//                Logs.d(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!subPackdsge wrong " + version + " " + headerLength);
//                return;
//            }
//
//            ipAndPort = packet.ip4Header.destinationAddress.getHostAddress() + ":" +
//                    packet.tcpHeader.destinationPort + ":" + packet.tcpHeader.sourcePort;
//        } else {
//            ipDataLen = mPendingBufferMap.len;
//            ipAndPort = mPendingBufferMap.ipAndPort;
//        }
//        TCB tcb = TCB.getTCB(ipAndPort);
//
//        if (ipDataLen < lenRemain) {
//            Logs.d(TAG, "ipDataLen < lenRemain");
//            if(mPendingBufferMap == null){
//                queueBuffer(tcb, ipDataLen, ByteBuffer.wrap(receiveBuffer, offset, lenRemain));
//                subPackge(receiveBuffer, offset + ipDataLen, lenRemain - ipDataLen);
//            } else {
//
//            }
//
//        } else if (ipDataLen > lenRemain) {
//            Logs.d(TAG, "!!!!!!!!!!!!!!!!!!ipDataLen > readLen , maybe need todo something!!!!!!!!!!!!!!!!! " + ipDataLen + " " + lenRemain);
//            if(pendingBuffer == null){
//                pendingBuffer = new PendingBuffer(ipDataLen);
//                mPendingBufferMap.put(ipAndPort, pendingBuffer);
//            }
//            pendingBuffer.append(receiveBuffer.array(), lenRemain);
//            return;
//        } else {
//            Logs.d(TAG, "subPackge end");
//            if(pendingBuffer != null){
//                pendingBuffer.append(receiveBuffer.array(), lenRemain);
//                receiveBuffer = ByteBuffer.wrap(pendingBuffer.buffer);
//                ipDataLen = pendingBuffer.len;
//            }
//            queueBuffer(tcb, ipDataLen, receiveBuffer);
//        }
//
//    }
//
//    private void queueBuffer(TCB tcb, int ipDataLen, ByteBuffer receiveBuffer){
//        synchronized (tcb) {
//            int readBytes = (ipDataLen - HEADER_SIZE);
//            Packet referencePacket = tcb.referencePacket;
//            // XXX: We should ideally be splitting segments by MTU/MSS, but this seems to work without
//            referencePacket.updateTCPBuffer(receiveBuffer, (byte) (Packet.TCPHeader.PSH | Packet.TCPHeader.ACK),
//                    tcb.mySequenceNum, tcb.myAcknowledgementNum, readBytes);
//            tcb.mySequenceNum += readBytes; // Next sequence number
//            receiveBuffer.position(0);
//            Logs.d(TAG, "processInputProxyMode " + tcb.status + " " + tcb.ipAndPort);
//            networkToDeviceQueue.offer(receiveBuffer);
//        }
//    }

    public class PendingBuffer {
        public byte[] buffer;
        public int len;
        public int bufferPosition;
        public String ipAndPort;

        public PendingBuffer(int pl, String ipport) {
            len = pl;
            buffer = new byte[len];
            bufferPosition = 0;
            ipAndPort = ipport;
        }

        public void append(byte[] src, int len) {
            System.arraycopy(buffer, bufferPosition, src, 0, len);
            bufferPosition += len;
        }
    }
}
