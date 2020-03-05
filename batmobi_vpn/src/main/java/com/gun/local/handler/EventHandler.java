package com.gun.local.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gun.local.tool.Logs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * description:事件处理，通过LocalBroadcast解耦
 * author: diff
 * date: 2018/1/8.
 */
public class EventHandler {
    private static final String TAG = "EventHandler";
    private static EventHandler instance;
    private BroadcastReceiver broadcastReceiver;
    private Context context;
    private Map<Class<IEventListener>, List<IEventListener>> eventMap = new HashMap<>();

    private EventHandler() {
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static EventHandler getInstance() {
        if (instance == null) {
            synchronized (EventHandler.class) {
                if (instance == null) {
                    instance = new EventHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化广播接收器
     */
    private void initBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String action = intent.getAction();
                    List<IEventListener> iEventListeners = getListeners((Class<IEventListener>) Class.forName(action));
                    if (iEventListeners == null) {
                        Logs.i("Event's listeners is empty");
                        return;
                    }
                    if (Events.EVENT_RECEIVED_HEART.getAction().equals(action)) {
                        /**处理心跳回复*/
                        int heartExtraValue = intent.getIntExtra("extra", -1);
                        boolean isException = intent.getBooleanExtra("exception", false);
                        for (IEventListener iEventListener : iEventListeners) {
                            if (iEventListener != null) {
                                ((IEventHeartReceivedListener) iEventListener).onHeartReceived(heartExtraValue, isException);
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 发送心跳已接收事件
     *
     * @param extraValue
     * @param isException
     */
    public void sendHeartReceivedEvent(int extraValue, boolean isException) {
        Intent intent = new Intent(Events.EVENT_RECEIVED_HEART.getAction());
        intent.putExtra("extra", extraValue);
        intent.putExtra("exception", isException);
        sendEvent(context, Events.EVENT_RECEIVED_HEART, intent);
    }

    /**
     * 发送local广播
     *
     * @param context
     * @param intent
     */
    public void sendEvent(Context context, Events events, Intent intent) {
        intent.setAction(events.getAction());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * 根据监听器的class获取设置所有观察者
     *
     * @param clazz
     * @return
     */
    private List<IEventListener> getListeners(Class<IEventListener> clazz) {
        eventMap.get(clazz);
        return eventMap.get(clazz);
    }

    /**
     * 注册
     *
     * @param context
     */
    public void register(Context context) {
        this.context = context;
        initBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(Events.EVENT_RECEIVED_HEART.getAction());
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * 添加一个心跳收到回复监听
     *
     * @param listener
     */
    public void addIEventHeartReceivedListener(IEventHeartReceivedListener listener) {
        addIEventListener(IEventHeartReceivedListener.class, listener);
    }

    /**
     * 添加事件监听器
     *
     * @param clazz
     * @param iEventListener
     */
    private void addIEventListener(Class clazz, IEventListener iEventListener) {
        Logs.i(TAG, "addIEventListener:" + clazz.getName());
        List<IEventListener> iEventListeners;
        if (eventMap.containsKey(clazz)) {
            iEventListeners = eventMap.get(clazz);
        } else {
            iEventListeners = new ArrayList<>();
            eventMap.put((Class<IEventListener>) clazz, iEventListeners);
        }
        iEventListeners.add(iEventListener);
    }

    /**
     * 取消注册
     *
     * @param context
     */
    public void unRegister(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    /**
     * 事件监听器父接口
     */
    public interface IEventListener {
    }

    /**
     * 心跳回复收到监听器
     */
    public interface IEventHeartReceivedListener extends IEventListener {
        void onHeartReceived(int heartExtraValue, boolean isException);
    }

    /**
     * 所有事件
     */
    public enum Events {
        EVENT_RECEIVED_HEART(IEventHeartReceivedListener.class),;

        //和此事件绑定的回调类
        private Class clazz;

        Events(Class<? extends IEventListener> clazz) {
            this.clazz = clazz;
        }

        public Class getClazz() {
            return clazz;
        }

        public String getAction() {
            return clazz.getName();
        }
    }
}
