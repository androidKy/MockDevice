package com.gun.local;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.gun.local.handler.CommonManager;
import com.gun.local.internal.SdkConfig;
import com.gun.local.tool.Logs;
import com.gun.local.tool.PrefUtils;

import static android.app.Activity.RESULT_OK;

/**
 * description:Vpn sdk api实现管理类
 * author: xiaodifu
 * date: 2017/8/23.
 */
public final class GunLib {

    private volatile static Context applicationContext = null;
    private static Handler handler;
    public static final int VPN_REQUEST_CODE = 0x0F;
    public static final String ACTION_UPDATE_LOG = "action_update_log";

    /**
     * 设置代理服务器
     *
     * @param context      应用上下文
     * @param proxyAddress 代理服务器地址
     * @param port         代理服务器端口
     * @param appkey       appkey
     */
    public static void setProxy(Context context, String proxyAddress, int port, String appkey) {
        if (context != null && !TextUtils.isEmpty(proxyAddress) && proxyAddress.trim().length() != 0) {
            applicationContext = context.getApplicationContext();
            handler = new Handler(Looper.getMainLooper());
            PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.PROXY_ADDRESS, proxyAddress);
            PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.PROXY_PORT, port);
            PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.KEY_APPKEY, appkey);
            SdkConfig.ISPROXYMODE = true;
        } else {
            throw new IllegalArgumentException("setProxy failed,wrong params");
        }
    }

    public static void setCountry(String country) {
        PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.KEY_COUNTRY, country);
    }

    public static void setReConnectB(boolean reConnectB) {
        PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.KEY_RECONNECT_B, reConnectB);
    }

    public static void setExeceptionReConnectB(boolean execeptionReConnectB) {
        PrefUtils.setCommonSP(applicationContext, PrefUtils.PrefKeys.KEY_EXCEPTION_RECONNECT_B, execeptionReConnectB);
    }

    public static void start(Activity activity) {
        Intent vpnIntent = VpnService.prepare(activity);
        Logs.i("start:" + (vpnIntent == null));
        if (vpnIntent != null) {
            activity.startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        } else {
            onActivityResult(activity, VPN_REQUEST_CODE, RESULT_OK, null);
        }
    }

    public static void onCreate(Context context) {
        CommonManager.getInstance().onCreate(context);
    }

    public static void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            activity.startService(new Intent(activity, LocalVPNService.class));
        }
    }

    public static void onDestory(Context context) {
        CommonManager.getInstance().onDestory(context);
    }

    public static Context getContext() {
        return applicationContext;
    }

    public static Handler getHandler() {
        return handler;
    }

    /**
     * 局域网调试
     *
     * @param localDebug
     */
    public static void setLocalDebug(boolean localDebug) {
        SdkConfig.LOCAL_DEBUG = localDebug;
    }

    /**
     * 获取已保存的代理服务器ip
     *
     * @return
     */
    public static String getProxyAddress(Context context) {
        if (context == null) return "";
        return PrefUtils.getCommonSP(context).getString(PrefUtils.PrefKeys.PROXY_ADDRESS, "");
    }

    /**
     * 获取以保存的代理服务器端口号
     *
     * @return
     */
    public static int getProxyPort(Context context) {
        if (context == null) return -1;
        return PrefUtils.getCommonSP(context).getInt(PrefUtils.PrefKeys.PROXY_PORT, -1);
    }

    /**
     * 获取已保存的代理服务器ip
     *
     * @return
     */
    public static String getAppkey() {
        return PrefUtils.getCommonSP(applicationContext).getString(PrefUtils.PrefKeys.KEY_APPKEY, "");
    }

    /**
     * 获取已保存的COUNTRY
     *
     * @return
     */
    public static String getCountry() {
        return PrefUtils.getCommonSP(applicationContext).getString(PrefUtils.PrefKeys.KEY_COUNTRY, "");
    }

    /**
     * 获取已保存的COUNTRY
     *
     * @return
     */
    public static boolean getReConnectB() {
        if (PrefUtils.getCommonSP(applicationContext) != null) {
            return PrefUtils.getCommonSP(applicationContext).getBoolean(PrefUtils.PrefKeys.KEY_RECONNECT_B, false);
        }
        return false;
    }

    /**
     * 获取已保存的COUNTRY
     *
     * @return
     */
    public static boolean getExceptionReConnectB() {
        return PrefUtils.getCommonSP(applicationContext).getBoolean(PrefUtils.PrefKeys.KEY_EXCEPTION_RECONNECT_B, false);
    }


    /**
     * 获取SDK名字
     *
     * @return
     */
    public static String getSdkName() {
        return SdkConfig.SDK_NAME;
    }

    /**
     * 获取SDK版本名
     *
     * @return
     */
    public static String getVersionName() {
        return SdkConfig.SDK_VERSION_NAME;
    }

    /**
     * 获取SDK版本号
     *
     * @return
     */
    public static int getVersionCode() {
        return SdkConfig.SDK_VERSION_CODE;
    }

    /**
     * 设置为调试模式（有日志）
     */
    public static void setDebug() {
        SdkConfig.DEBUG = true;
    }
}
