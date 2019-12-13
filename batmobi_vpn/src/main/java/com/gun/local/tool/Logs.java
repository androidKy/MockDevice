package com.gun.local.tool;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.gun.local.GunLib;
import com.gun.local.internal.SdkConfig;


/**
 * description:日志输出类
 * author: xiaodifu
 * date: 2017/8/23.
 */
public class Logs {

    private static final String TAG = "GunSDK_A";

    public static void i(String msg){
        i("",msg);
    }

    public static void i(String tag, String msg) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.i(TAG, String.format("[%s]%s",tag,msg));
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.i(TAG,String.format("[%s]%s",tag,msg), tr);
    }

    public static void w(String msg){
        w("",msg);
    }

    public static void w(String tag, String msg) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.w(TAG, String.format("[%s]%s",tag,msg));
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.w(TAG, String.format("[%s]%s",tag,msg), tr);
    }

    public static void d(String msg){
        d("",msg);
    }

    public static void d(String tag, String msg) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.d(TAG, String.format("[%s]%s",tag,msg));
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.d(TAG, String.format("[%s]%s",tag,msg), tr);
    }

    public static void e(String msg){
        e("",msg);
    }

    public static void e(String tag, String msg) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.e(TAG, String.format("[%s]%s",tag,msg));
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (!SdkConfig.DEBUG) return;
        showLogs(tag, msg);
        Log.e(TAG, String.format("[%s]%s",tag,msg), tr);
    }

    private static void showLogs(String tag, String msg) {
        LocalBroadcastManager.getInstance(GunLib.getContext()).sendBroadcast(new Intent(GunLib.ACTION_UPDATE_LOG).putExtra("log", new StringBuilder(tag).append(":").append(msg).toString()));
    }

    public static void t(final String msg) {
        try {
            GunLib.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(GunLib.getContext(), msg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
