package com.gun.local.tool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * description:
 * author: xiaodifu
 * date: 2017/12/7.
 */
public class ThreadPool {
    private static ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(5);


    /**
     * 执行任务
     *
     * @param task
     */
    public static void execute(Runnable task) {
        scheduledThreadPool.execute(task);
    }

    public static void execute(Runnable task, long delay) {
        scheduledThreadPool.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

}
