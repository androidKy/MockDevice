package com.gun.local.handler;

import android.text.TextUtils;

import com.gun.local.GunLib;
import com.gun.local.report.ExceptionReporter;
import com.gun.local.tool.Logs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * description:异常集中处理器
 * author: diff
 * date: 2018/1/6.
 */
public class SocketExceptionHandler {
    private static final String TAG = "SocketExceptionHandler";
    private long lastCheckTime;
    private static SocketExceptionHandler instance;
    private List<ISocketExceptionListener> ISocketExceptionListeners;

    private SocketExceptionHandler() {
        ISocketExceptionListeners = new ArrayList<>();
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static SocketExceptionHandler getInstance() {
        if (instance == null) {
            synchronized (SocketExceptionHandler.class) {
                if (instance == null) {
                    instance = new SocketExceptionHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 添加一个异常
     *
     * @param e
     */
    public void addException(Throwable e) {
        if (e != null && !TextUtils.isEmpty(e.getMessage())) {
            new ExceptionReporter(e.getMessage(), GunLib.getProxyAddress(GunLib.getContext())).report();
        }
        if (System.currentTimeMillis() - lastCheckTime < 5 * 1000) {
            Logs.i(TAG, "异常检测过于频繁，最小间隔五秒触发");
            return;
        }
        checkException(e);
    }

    /**
     * 处理异常
     *
     * @param throwable
     */
    private void checkException(Throwable throwable) {
        lastCheckTime = System.currentTimeMillis();
        for (ISocketExceptionListener iSocketExceptionListener : ISocketExceptionListeners) {
            if (iSocketExceptionListener != null) {
                iSocketExceptionListener.needCheckSocketAlive();
            }
        }
    }

    /**
     * 添加一个Socket异常监听器
     *
     * @param iSocketExceptionListener
     */
    public void addISocketExceptionListener(ISocketExceptionListener iSocketExceptionListener) {
        ISocketExceptionListeners.add(iSocketExceptionListener);
    }

    /**
     * Socket异常监听器
     */
    public interface ISocketExceptionListener {
        void needCheckSocketAlive();
    }
}
