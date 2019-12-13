package com.gun.local.handler;

import android.util.SparseBooleanArray;

import com.gun.local.internal.GunExecutor;
import com.gun.local.internal.Packet;
import com.gun.local.internal.Protocol;
import com.gun.local.tool.Logs;
import com.gun.local.tool.StupidUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * description:智能心跳
 * author: diff
 * date: 2017/12/27.
 */
public class HeartBeatHandler {
    private static final String TAG = "HeartBeatHandler";
    /**
     * 最大心跳间隔:3分钟
     */
    private static final long MAX_VALUE = 60 * 1000L;
    /**
     * 最小心跳间隔：30s
     */
    private static final long MIN_VALUE = 5 * 1000L;
    /**
     * 默认心跳间隔：30s
     */
    private static final long DEFAULT_VALUE = 30 * 1000L;
    /**
     * 最优心跳阈值：5s（两次值的间隔小于此值表示已找到最优心跳）
     */
    private static final long DEFAULT_THRESHOLD = 5 * 1000L;
    /**
     * 异常心跳响应超时：5s
     */
    private static final long EXCEPTION_TIME_OUT = 5 * 1000L;
    /**
     * 单例
     */
    private static HeartBeatHandler instance;
    /**
     * 当前心跳
     */
    private long currentValue;
    /**
     * 调整值
     */
    private long changeValue;
    /**
     * 是否已找到最优心跳值
     */
    private boolean isBestValue = false;
    /**
     * 是否已定时通过最优心跳值发送心跳
     */
    private boolean isSchedule = false;
    /**
     * 发送心跳的任务
     */
    private volatile SendHeartBeatRunnable sendHeartBeatRunnable;
    /**
     * 心跳超时处理的任务
     */
    private volatile HeartResponeTimeOutHandler heartTimeOutHandler;
    private volatile HeartResponeTimeOutHandler exceptionHeartTimeOutHandler;
    /**
     * 心跳包的额外值，用于区分是否是当前心跳（因为可能会因为网络延时，收到之前的心跳回复）
     */
    private int heartExtraValue;
    /**
     * 超时监听器
     */
    private ITimeOutListener iTimeOutListener;
    /**
     * 上次心跳的延时
     */
    private long lastHeartDelay = -3;

    private SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();

    private SocketChannel socketChannel;

    private HeartBeatHandler() {
        /**初始当前值为最大值和最小值的中间值*/
        currentValue = (MIN_VALUE + MAX_VALUE) / 2;
        /** 初始改变值为最大值与最小值的差除以2*/
        changeValue = (MAX_VALUE - MIN_VALUE) / 2;
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static HeartBeatHandler getInstance() {
        if (instance == null) {
            synchronized (HeartBeatHandler.class) {
                if (instance == null) {
                    instance = new HeartBeatHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 重置心跳（不改变之前的智能心跳值）
     *
     * @param socketChannel
     */
    public void restart(SocketChannel socketChannel) {
        setSocketChannel(socketChannel);
        if (sendHeartBeatRunnable != null) {
            GunExecutor.getInstance().getScheduledThreadPoolExecutor().remove(sendHeartBeatRunnable);
        }
        if (heartTimeOutHandler != null) {
            GunExecutor.getInstance().getScheduledThreadPoolExecutor().remove(heartTimeOutHandler);
        }
        if (exceptionHeartTimeOutHandler != null) {
            GunExecutor.getInstance().getScheduledThreadPoolExecutor().remove(exceptionHeartTimeOutHandler);
        }
    }

    public void setTimeOutListener(ITimeOutListener iTimeOutListener) {
        this.iTimeOutListener = iTimeOutListener;
    }

    /**
     * 获取默认心跳值
     *
     * @return
     */
    public long getDefaultValue() {
        return DEFAULT_VALUE;
    }

    /**
     * 获取心跳值
     *
     * @return
     */
    private long getHeartValue() {
        return currentValue;
    }

    /**
     * 是否是最佳心跳值
     *
     * @return
     */
    private boolean isBestValue() {
        return isBestValue;
    }

    /**
     * 处理心跳接收，每次收到之后再发送下一次
     *
     * @param receivedTime
     * @param extraData
     * @param isException
     */
    public void handleHeartData(long receivedTime, int extraData, boolean isException) {
        Logs.i(TAG, "handleHeartData:" + extraData + "," + isException);
        sparseBooleanArray.put(extraData, false);
        if (isException && extraData == getExceptionHeartExtraValue()) {
            Logs.i(TAG, "是异常心跳回包，remove掉超时处理任务");
            GunExecutor.getInstance().getScheduledThreadPoolExecutor().remove(exceptionHeartTimeOutHandler);
        } else {
            Logs.i(TAG, "收到的extra：" + extraData + ",当前的extra：" + heartTimeOutHandler.getExtraValue());
            if (extraData == heartTimeOutHandler.getExtraValue()) {
                /**上次心跳延时*/
                lastHeartDelay = sendHeartBeatRunnable.getSendTime() == 0 ? -3 : receivedTime - sendHeartBeatRunnable.getSendTime();
                Logs.i(TAG, "移除超时任务");
                //移除超时处理任务
                GunExecutor.getInstance().getScheduledThreadPoolExecutor().remove(heartTimeOutHandler);
                //处理智能心跳调整
                handleEvent(HeartEvent.INCREASE);
                //发送下一次心跳
                sendHeartBeat(false);
            }
        }
    }

    /**
     * 心跳事件处理
     *
     * @param heartEvent
     */
    private synchronized void handleEvent(HeartEvent heartEvent) {
        if (isBestValue) {
            return;
        }
        switch (heartEvent) {
            case INCREASE:
                Logs.i(TAG, "handleEvent:INCREASE");
                /**如果改变值小于心跳阈值，则表示已找到最优心跳值*/
                if (changeValue <= DEFAULT_THRESHOLD) {
                    isBestValue = true;
                    return;
                }
                changeValue = changeValue / 2;
                currentValue = currentValue + changeValue;
                break;
            case DISCREASE:
                Logs.i(TAG, "handleEvent:DISCREASE");
                changeValue = changeValue / 2;
                currentValue = currentValue - changeValue;
                break;
        }
    }

    public void setSocketChannel(SocketChannel channel) {
        this.socketChannel = channel;
    }

    /**
     * 发送心跳包
     *
     * @param isException 是否是异常心跳
     */
    public void sendHeartBeat(boolean isException) {
        try {
            Logs.i(TAG, "sendHeartBeat:心跳值：" + getHeartValue() + "," + isException);
            if (sendHeartBeatRunnable == null) {
                sendHeartBeatRunnable = new SendHeartBeatRunnable(false);
            }
            if (heartTimeOutHandler == null) {
                heartTimeOutHandler = new HeartResponeTimeOutHandler();
            }
            if (exceptionHeartTimeOutHandler == null) {
                exceptionHeartTimeOutHandler = new HeartResponeTimeOutHandler();
            }

            if (isException) {
                /**发送异常心跳*/
                GunExecutor.getInstance().getThreadPoolExecutor().execute(new SendHeartBeatRunnable(true));
                return;
            }

            if (isBestValue()) {
                if (isSchedule) {
                    Logs.i(TAG, "已是最优心跳，return," + getHeartValue());
                    return;
                }
                Logs.i(TAG, "已是最优心跳，开始定时任务，心跳值：" + getHeartValue());
                isSchedule = true;
                GunExecutor.getInstance().getScheduledThreadPoolExecutor().scheduleWithFixedDelay(sendHeartBeatRunnable, getHeartValue(), getHeartValue(), TimeUnit.MILLISECONDS);
            } else {
                Logs.i(TAG, "还不是最优心跳，做单个任务，心跳值：" + getHeartValue());
                GunExecutor.getInstance().getScheduledThreadPoolExecutor().schedule(sendHeartBeatRunnable, getHeartValue(), TimeUnit.MILLISECONDS);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private long getLastHeartDelay() {
        return lastHeartDelay;
    }

    /**
     * 改变心跳额外值
     */
    private synchronized void changeHeartExtraValue() {
        if (heartExtraValue < Integer.MAX_VALUE) {
            ++heartExtraValue;
        } else {
            heartExtraValue = 0;
            sparseBooleanArray = new SparseBooleanArray();
        }
        sparseBooleanArray.put(heartExtraValue, true);
    }

    /**
     * 获取心跳额外值
     *
     * @return
     */
    public int getHeartExtraValue() {
        return heartExtraValue;
    }

    /**
     * 获取异常心跳额外值
     *
     * @return
     */
    public int getExceptionHeartExtraValue() {
        return Integer.MAX_VALUE;
    }

    /**
     * 发送心跳包的任务
     */
    private static class SendHeartBeatRunnable implements Runnable {
        private boolean isException;
        private long sendTime;

        public SendHeartBeatRunnable(boolean isException) {
            this.isException = isException;
        }

        public long getSendTime() {
            return sendTime;
        }

        @Override
        public void run() {
            /**改变心跳额外值*/
            HeartBeatHandler.getInstance().changeHeartExtraValue();
            ByteBuffer receiveBuffer = ByteBuffer.allocate(Packet.HEADER_SIZE);
            receiveBuffer.put(Protocol.HEART_A);
            // 添加心跳标志值
            receiveBuffer.putInt(isException ? HeartBeatHandler.getInstance().getExceptionHeartExtraValue() : HeartBeatHandler.getInstance().getHeartExtraValue());
            // 添加是否是异常确认包
            receiveBuffer.put(StupidUtil.intToByte(isException ? 1 : 2));
            // 添加上次心跳延时
            Logs.i(TAG, "SendHeartBeatRunnable：上次心跳延时：" + HeartBeatHandler.getInstance().getLastHeartDelay());
            receiveBuffer.putLong(HeartBeatHandler.getInstance().getLastHeartDelay());
            receiveBuffer.position(0);
            receiveBuffer.limit(Packet.HEADER_SIZE);
            try {
                HeartBeatHandler.getInstance().socketChannel.write(receiveBuffer);
                //记录本次心跳的发送时间
                sendTime = System.currentTimeMillis();
                Logs.i("HeartBeatHandler", "发送A心跳：" + HeartBeatHandler.getInstance().getHeartExtraValue());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            /**开启延时任务,超时处理:异常为5s，非异常为当前心跳包时间*/
            HeartResponeTimeOutHandler heartResponeTimeOutHandler = isException ? HeartBeatHandler.getInstance().exceptionHeartTimeOutHandler : HeartBeatHandler.getInstance().heartTimeOutHandler;
            int extraValue = isException ? HeartBeatHandler.getInstance().getExceptionHeartExtraValue() : HeartBeatHandler.getInstance().getHeartExtraValue();
            long timeout = isException ? HeartBeatHandler.EXCEPTION_TIME_OUT : HeartBeatHandler.getInstance().getHeartValue();
            GunExecutor.getInstance().getScheduledThreadPoolExecutor().schedule(heartResponeTimeOutHandler.setExtraValue(extraValue), timeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 服务器心跳回复超时处理任务
     */
    private static class HeartResponeTimeOutHandler implements Runnable {
        private int extraValue;

        public HeartResponeTimeOutHandler setExtraValue(int extraValue) {
            this.extraValue = extraValue;
            return this;
        }

        public int getExtraValue() {
            return extraValue;
        }

        @Override
        public void run() {
            if (HeartBeatHandler.getInstance().sparseBooleanArray.get(extraValue)) {
                Logs.i(TAG, "HeartResponeTimeOutHandler,超时，" + extraValue);
                //改变下一次的心跳额外值
                HeartBeatHandler.getInstance().changeHeartExtraValue();
                //将下一次的心跳间隔缩短
                HeartBeatHandler.getInstance().handleEvent(HeartEvent.DISCREASE);
                //回调超时监听
                if (HeartBeatHandler.getInstance().iTimeOutListener != null) {
                    HeartBeatHandler.getInstance().iTimeOutListener.onTimeOut(extraValue);
                }
            }
        }
    }

    /**
     * 超时监听器
     */
    public interface ITimeOutListener {
        void onTimeOut(int extra);
    }

    /**
     * 心跳事件
     */
    public enum HeartEvent {
        INCREASE,
        DISCREASE
    }

}
