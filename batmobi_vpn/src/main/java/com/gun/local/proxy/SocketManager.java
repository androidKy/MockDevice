package com.gun.local.proxy;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**
 * description:
 * author: kyXiao
 * date: 2019/4/2
 */
public class SocketManager {
    private static final String TAG = "SocketManager";

    //todo windows上的ip和服务端口暂时写死，到时由服务器下发
    private static final String SERVER_IP = "192.168.2.105";
    private static final int SERVER_PORT = 9999;
    private String mCountry;
    private int mPort;
    private Thread mThread;
    private OutputStream mOutputStream;
    private BufferedReader mBufferedReader;
    private Socket mSocket;
    private Handler mThreadHandler;

    private static final int MSG_START_CONNECT = 100;
    private static final int MSG_SEND_MSG = 200;
    private static final int MSG_DISCONNECT = 400;

    public SocketManager(String country, int port) {
        this.mCountry = country;
        this.mPort = port;
        loopThread();
    }

    private void loopThread() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }

        mThread = new Thread(new Runnable() {

            @SuppressLint("HandlerLeak")
            @Override
            public void run() {
                Looper.prepare();

                mThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case MSG_START_CONNECT:
                                connectServer();
                                break;
                            case MSG_SEND_MSG:
                                sendMsg2Server(msg.getData().getString("msg"));
                                break;
                            case MSG_DISCONNECT:
                                closeSocket();
                                break;

                        }
                    }
                };
                mThreadHandler.sendEmptyMessage(MSG_START_CONNECT);

                Looper.loop();
            }
        });
        mThread.start();
    }

    /**
     * 发送消息到windows上
     */
    private void connectServer() {
        Log.i(TAG, "connectServer: ");
        //与服务器建立连接
        try {
            mSocket = new Socket(SERVER_IP, SERVER_PORT);
            InputStream inputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();

            //获取服务器发送来的数据
            mBufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            char[] buffer = new char[256];
            StringBuilder bufferString = new StringBuilder();
            int conut = 0;
            int tag = 0;

           /* while (true) {
                while (mBufferedReader.read(buffer) > 0) {
                    while (tag < buffer.length) {
                        bufferString.append(buffer[tag]);
                        tag++;
                    }
                    tag = 0;
                    bufferString = new StringBuilder();

                    Log.i(TAG, "sendMsg: " + bufferString);
                }
            }*/
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void responFailed() {
        if (mMsgListener != null)
            mMsgListener.onSendFailed();
    }

    private void responSucceed() {
        if (mMsgListener != null)
            mMsgListener.onSentSucceed();
    }

    /**
     * 发送消息给服务器
     *
     * @param msg
     */
    private void sendMsg2Server(String msg) {
        Log.i(TAG, "sendMsg2Server: ");
        if (TextUtils.isEmpty(msg)) {
            Log.i(TAG, "sendMsg2Server: msg is empty");
            responFailed();
            return;
        }

        if (mOutputStream != null) {
            try {
                mOutputStream.write((msg + "\n").getBytes());
                mOutputStream.flush();
                responSucceed();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                //发送消息后断开连接
                mThreadHandler.sendEmptyMessageDelayed(MSG_DISCONNECT, 2 * 1000);
            }
        } else {
            closeSocket();
            responFailed();
        }
    }

    private void closeSocket() {
        Log.i(TAG, "closeSocket: ");
        try {
            if (mThreadHandler != null)
                mThreadHandler.removeCallbacksAndMessages(null);
            if (mSocket != null)
                mSocket.close();
            if (mBufferedReader != null)
                mBufferedReader.close();
            if (mOutputStream != null)
                mOutputStream.close();

          /*  if (mThread != null)
                mThread.interrupt();*/

        } catch (Exception e) {
            Log.e(TAG, "closeSocket: " + e.getMessage(), e);
        }
    }


    public void sendMsg(ProxyOperate proxyOperate, MsgListener msgListener) {
        mMsgListener = msgListener;

        if (TextUtils.isEmpty(mCountry) || mPort == 0) {
            Log.i(TAG, "sendMsg: country can not be null and port != 0");
            closeSocket();
            responFailed();
            return;
        }

        String msg = "";
        if (proxyOperate == ProxyOperate.FREE_PROXY) {
            msg = "Autoproxytool.exe -changeproxy/" + mCountry + " -freeport=" + mPort;
        } else msg = "Autoproxytool.exe -changeproxy/" + mCountry + " -proxyport=" + mPort;

        //  msg = "test data";
        final String finalMsg = msg;
        //必须延时等线程创建mThreadHandler
        mMainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mThreadHandler == null) {
                    Log.i(TAG, "sendMsg: 线程还没创建");
                    closeSocket();
                    responFailed();
                    return;
                }
                Log.i(TAG, "mMainHandler: ");
                Message message = mThreadHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("msg", finalMsg);
                message.setData(bundle);
                message.what = MSG_SEND_MSG;
                mThreadHandler.sendMessage(message);
            }
        }, 500);
    }

    private Handler mMainHandler = new Handler(Looper.getMainLooper());
    private MsgListener mMsgListener;

    public interface MsgListener {
        void onSentSucceed();

        void onSendFailed();
    }
}
