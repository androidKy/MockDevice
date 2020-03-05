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

package com.gun.local;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gun.local.internal.ByteBufferPool;
import com.gun.local.internal.GunExecutor;
import com.gun.local.internal.Packet;
import com.gun.local.internal.TCPInput;
import com.gun.local.internal.TCPOutput;
import com.gun.local.internal.UDPInput;
import com.gun.local.internal.UDPOutput;
import com.gun.local.internal.SdkConfig;
import com.gun.local.tool.Logs;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.LinkedBlockingQueue;

public class LocalVPNService extends VpnService {
    private static final String TAG = "LocalVPNService";
    private static final String VPN_ADDRESS = "10.0.0.2"; // Only IPv4 support for now
    private static final String VPN_ROUTE = "0.0.0.0"; // Intercept everything

    public static final String BROADCAST_VPN_STATE = "com.unit.local.localvpn.VPN_STATE";
    public static final String BROADCAST_CLOSE_VPN = "com.unit.local.localvpn.CLOSE_VPN";
    public static final String ACTION_CONNECTED = "com.gun.vpn.local.ACTION_CONNECTED";

    private static boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;
    private Thread vpnThread = null;
    private Thread udpInputThread = null;
    private Thread udpOutputThread = null;
    private Thread tcpInputThread = null;
    private Thread tcpOutputThread = null;

    private PendingIntent pendingIntent;

    private LinkedBlockingQueue<Packet> deviceToNetworkUDPQueue;
    private LinkedBlockingQueue<Packet> deviceToNetworkTCPQueue;
    private LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue;

    private Selector udpSelector;
    private Selector tcpSelector;
    private TCPOutput tcpOutput;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BROADCAST_CLOSE_VPN.equals(intent.getAction())) {
                stopVPN();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        setupVPN();
        try {
            udpSelector = Selector.open();
            tcpSelector = Selector.open();
            deviceToNetworkUDPQueue = new LinkedBlockingQueue<>();
            deviceToNetworkTCPQueue = new LinkedBlockingQueue<>();
            networkToDeviceQueue = new LinkedBlockingQueue<>();

            udpInputThread = new Thread(new UDPInput(networkToDeviceQueue, udpSelector));
            udpInputThread.start();
            udpOutputThread = new Thread(new UDPOutput(deviceToNetworkUDPQueue, udpSelector, this));
            udpOutputThread.start();
            tcpInputThread = new Thread(new TCPInput(networkToDeviceQueue, tcpSelector));
            tcpInputThread.start();
            tcpOutputThread = new Thread(tcpOutput = new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this));
            tcpOutputThread.start();

            vpnThread = new Thread(new VPNRunnable(vpnInterface.getFileDescriptor(),
                    deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
            vpnThread.start();

            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("running", true));
        } catch (IOException e) {
            // TODO: Here and elsewhere, we should explicitly notify the user of any errors
            // and suggest that they stop the service, since we can't do it ourselves
            Log.e(TAG, "Error starting service", e);
            cleanup();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_CLOSE_VPN);
        registerReceiver(receiver, filter);
    }

    private void setupVPN() {
        if (vpnInterface == null) {
            Builder builder = new Builder();
            builder.addAddress(VPN_ADDRESS, 32);
            builder.addRoute(VPN_ROUTE, 0);
            vpnInterface = builder.setSession(getPackageName()).setConfigureIntent(pendingIntent).establish();
        }
    }

    private void stopVPN() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                vpnThread.interrupt();
                udpInputThread.interrupt();
                udpOutputThread.interrupt();
                tcpInputThread.interrupt();
                tcpOutputThread.interrupt();

                sendClosePacket();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GunExecutor.getInstance().shutdownExecutors();
                cleanup();
            }
        }).start();
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        isRunning = false;
        LocalBroadcastManager.getInstance(LocalVPNService.this).
                sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("running", false));
    }

    private void sendClosePacket() {
        try {
            tcpOutput.sendClosePacket();
        } catch (Throwable e) {
            e.printStackTrace();
            Logs.e(TAG, "sendClosePacket exception:" + e.getMessage());
        }
    }

    private void cleanup() {
        try {
            tcpOutput.close();
            deviceToNetworkTCPQueue = null;
            deviceToNetworkUDPQueue = null;
            networkToDeviceQueue = null;
            ByteBufferPool.clear();
            closeResources(udpSelector, tcpSelector, vpnInterface);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    // TODO: Move this to a "utils" class for reuse
    private static void closeResources(Closeable... resources) {
        for (Closeable resource : resources) {
            try {
                if (resource == null) continue;
                resource.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static class VPNRunnable implements Runnable {
        private static final String TAG = VPNRunnable.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private LinkedBlockingQueue<Packet> deviceToNetworkUDPQueue;
        private LinkedBlockingQueue<Packet> deviceToNetworkTCPQueue;
        private LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue;

        public VPNRunnable(FileDescriptor vpnFileDescriptor,
                           LinkedBlockingQueue<Packet> deviceToNetworkUDPQueue,
                           LinkedBlockingQueue<Packet> deviceToNetworkTCPQueue,
                           LinkedBlockingQueue<ByteBuffer> networkToDeviceQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        @Override
        public void run() {
            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();

            try {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived;
                while (!Thread.interrupted()) {
                    if (dataSent)
                        bufferToNetwork = ByteBufferPool.acquire();
                    else
                        bufferToNetwork.clear();

                    // TODO: Block when not connected
                    int readBytes = vpnInput.read(bufferToNetwork);

                    if (readBytes > 0) {
                        dataSent = true;
                        bufferToNetwork.flip();
                        Packet packet = new Packet(bufferToNetwork);


                        if (SdkConfig.ISPROXYMODE && packet.isTCP()) {
//                            if(bufferToNetwork.position()!=bufferToNetwork.limit()){
                            bufferToNetwork.rewind();
//                                byte[] buffer = new byte[16384];
//                                buffer = bufferToNetwork.array().clone();
//                                int bufferLen = bufferToNetwork.limit();
//                                Logs.d(TAG,bufferToNetwork.position()+" "+bufferToNetwork.limit());
//                                bufferToNetwork.limit(16384);
//                                bufferToNetwork.put(buffer, 0, bufferLen);
//
//                                bufferToNetwork.flip();
//                                packet = new Packet(bufferToNetwork);
//                                Logs.d(TAG,bufferToNetwork.position()+" "+bufferToNetwork.limit());
//                            }
                        }


                        if (packet.isUDP()) {
//                            Logs.d(TAG, "udp found!!!!!!,unsurpport for proxy mode,note!!!!!!!!");
                            deviceToNetworkUDPQueue.offer(packet);
                        } else if (packet.isTCP()) {
                            deviceToNetworkTCPQueue.offer(packet);
                        } else {
                            Logs.w(TAG, packet.ip4Header.toString());
                            dataSent = false;
                        }
                    } else {
                        dataSent = false;
                    }

                    ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();
                    if (bufferFromNetwork != null) {
                        synchronized (networkToDeviceQueue) {
                            bufferFromNetwork.flip();
                            while (bufferFromNetwork.hasRemaining())
                                vpnOutput.write(bufferFromNetwork);
                            dataReceived = true;

                            ByteBufferPool.release(bufferFromNetwork);
                        }
                    } else {
                        dataReceived = false;
                    }

                    // TODO: Sleep-looping is not very battery-friendly, consider blocking instead
                    // Confirm if throughput with ConcurrentQueue is really higher compared to BlockingQueue
                    if (!dataSent && !dataReceived)
                        Thread.sleep(10);
                }
            } catch (InterruptedException e) {
            } catch (IOException e) {
                Logs.w(TAG, e.toString(), e);
            } finally {
                closeResources(vpnInput, vpnOutput);
            }
        }
    }
}
