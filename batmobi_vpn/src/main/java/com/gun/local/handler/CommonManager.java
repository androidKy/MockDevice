package com.gun.local.handler;

import android.content.Context;

import com.gun.local.tool.Logs;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * description:所有Handler管理
 * author: diff
 * date: 2018/1/8.
 */
public class CommonManager implements HeartBeatHandler.ITimeOutListener, SocketExceptionHandler.ISocketExceptionListener {
    private static final String TAG = "CommonManager";
    private static CommonManager instance;
    private List<ISocketListener> socketListeners = new ArrayList<>();

    private CommonManager() {
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static CommonManager getInstance() {
        if (instance == null) {
            synchronized (CommonManager.class) {
                if (instance == null) {
                    instance = new CommonManager();
                }
            }
        }
        return instance;
    }

    /**
     * 设置Socket需要重连的监听器
     *
     * @param iSocketListener
     */
    public void addISocketListener(ISocketListener iSocketListener) {
        Logs.i(TAG, "addISocketListener()");
        socketListeners.add(iSocketListener);
    }

    /**
     * 发送事件：心跳已接收
     *
     * @param extraValue
     * @param isException
     */
    public void sendHeartReceivedEvent(int extraValue, boolean isException) {
        Logs.i(TAG, "sendHeartReceivedEvent()," + extraValue + "," + isException);
        EventHandler.getInstance().sendHeartReceivedEvent(extraValue, isException);
    }

    /**
     * 发送心跳包
     *
     * @param isException 是否是异常心跳包
     */
    public void sendHeart(boolean isException) {
        Logs.i(TAG, "sendHeart()," + isException);
        HeartBeatHandler.getInstance().sendHeartBeat(isException);
    }

    /**
     * 设置心跳处理器的socketchannel
     *
     * @param socketChannel
     */
    public void setHeartSocketChannel(SocketChannel socketChannel) {
        Logs.i(TAG, "setHeartSocketChannel");
        HeartBeatHandler.getInstance().setSocketChannel(socketChannel);
    }

    /**
     * 创建
     *
     * @param context
     */
    public void onCreate(Context context) {
        Logs.i(TAG, "onCreate()");
        EventHandler.getInstance().register(context);
        HeartBeatHandler.getInstance().setTimeOutListener(this);
        SocketExceptionHandler.getInstance().addISocketExceptionListener(this);
    }

    /**
     * 重置智能心跳设置
     *
     * @param socketChannel
     */
    public void restartHeart(SocketChannel socketChannel) {
        Logs.i(TAG, "restartHeart()");
        HeartBeatHandler.getInstance().restart(socketChannel);
    }

    /**
     * 添加一个捕获的socket异常
     *
     * @param e
     */
    public void addSocketException(Throwable e) {
        Logs.i(TAG, "addSocketException()");
        SocketExceptionHandler.getInstance().addException(e);
    }

    /**
     * 销毁
     *
     * @param context
     */
    public void onDestory(Context context) {
        Logs.i(TAG, "onDestory()");
        EventHandler.getInstance().unRegister(context);
    }

    /**
     * 处理收到server回包
     *
     * @param receivedTime
     * @param heartExtraValue
     * @param isException
     */
    public void onHeartReceived(long receivedTime, int heartExtraValue, boolean isException) {
        Logs.i(TAG, "onHeartReceived:" + heartExtraValue + "," + isException);
        HeartBeatHandler.getInstance().handleHeartData(receivedTime, heartExtraValue, isException);
    }

    @Override
    public void onTimeOut(int extra) {
        Logs.i(TAG, "onTimeOut()," + extra);
        for (ISocketListener iSocketListener : socketListeners) {
            iSocketListener.reconnect();
        }
    }

    @Override
    public void needCheckSocketAlive() {
        Logs.i(TAG, "needCheckSocketAlive()");
        //Socket异常处理器回调，需要发送心跳确认socket是否还可用
        sendHeart(true);
    }

    /**
     * Socket需要重连的监听器
     */
    public interface ISocketListener {
        void reconnect();
    }
}
