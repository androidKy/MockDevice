package com.gun.local.internal;

import android.os.Build;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * description:线程池
 * author: diff
 * date: 2017/12/28.
 */
public class GunExecutor {
    private ScheduledExecutorService scheduledExecutorService;
    private Executor executor;
    private static GunExecutor instance;

    private GunExecutor() {
    }

    public static GunExecutor getInstance() {
        if (instance == null) {
            instance = new GunExecutor();
        }
        return instance;
    }

    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        if (this.scheduledExecutorService == null || this.scheduledExecutorService.isShutdown() || this.scheduledExecutorService.isTerminated()) {
            this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
        }
        return (ScheduledThreadPoolExecutor) this.scheduledExecutorService;
    }

    public Executor getThreadPoolExecutor() {
        if(this.executor == null || this.executor instanceof ThreadPoolExecutor && (((ThreadPoolExecutor)this.executor).isShutdown() || ((ThreadPoolExecutor)this.executor).isTerminated() || ((ThreadPoolExecutor)this.executor).isTerminating())) {
            if(Build.VERSION.SDK_INT < 11) {
                return Executors.newSingleThreadExecutor();
            }

            this.executor = Executors.newFixedThreadPool(2);
        }

        return this.executor;
    }

    public void shutdownExecutors() {
        try {
            this.stopExecutorService(this.scheduledExecutorService);

        } catch (Throwable e) {
        }
    }

    private void stopExecutorService(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(10L, TimeUnit.SECONDS);
            return;
        } catch (InterruptedException var5) {
        } finally {
            if (!executor.isTerminated()) {
            }

            executor.shutdownNow();
        }

    }
}
