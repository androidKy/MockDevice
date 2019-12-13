package com.gun.local.report;

import android.content.Context;
import android.os.Build;


import com.gun.local.internal.SdkConfig;
import com.gun.local.tool.LibUtil;

import org.json.JSONObject;

/**
 * description:上传参数拼接器
 * author: xiaodifu
 * date: 2017/12/11.
 */
public class ServerParams {
    static final int P_VERSION = 2;//v272版本开始=2,2017-09-02
    static final String PARAM_PVERSION = "pv";
    static final String PARAM_REQUEST_ID = "rqd";//system current millis
    static final String PARAM_UA = "ua";//user agent
    static final String PARAM_ADV_ID = "gd";//gaid
    static final String PARAM_AID = "ud";//android id
    static final String PARAM_LOCAL = "co";//country
    static final String PARAM_LANG = "ln";//language
    static final String PARAM_SYS_NAME = "sn";//system name
    static final String PARAM_SYS_CODE = "sc";//system code
    static final String PARAM_PKG_NAME = "pn";//package name
    static final String PARAM_CVERSION = "cv";//app version code
    static final String PARAM_CVNAME = "cn";//app version name
    static final String PARAM_SDK_NAME = "dn";//sdk version name
    static final String PARAM_SDK_CODE = "so";//sdk version code
    static final String PARAM_NET_TYPE = "nt";//network type
    static final String PARAM_SCREEN_SIZE = "ss";//screen size
    static final String PARAM_RAM = "rm";//ram
    static final String PARAM_IS_TABLET = "st";//device type,0手机，1平板
    static final String PARAM_OPERATOR = "op";//运营商
    static final String PARAM_ROM = "ro";//rom 大小
    static final String PARAM_CPU = "cu";//cpu个数
    static final String PARAM_TZ = "tz";//时区
    static final String PARAM_MODE = "m";//机型
    static final String PARAM_CDAYS = "cs";//安装天数
    static final String PARAM_PLATFORM = "af";//平台（android）
    static final String PARAM_IS_GP_INSTALLED = "igi";//是否安装了GP
    static final String PARAM_IS_ROOT = "irt";//是否root
    static final String PARAM_SR = "sr";//请求来源 0.dsp sdk 1.应用墙SDK 2.原生广告SDK
    static final String PARAM_CMD5 = "cmd5";//宿主apk包的MD5签名16进制大写字符串

    /**
     * 构建公共头信息
     *
     * @return
     */
    public static JSONObject buildHeader(Context context) {
        JSONObject pHeader = new JSONObject();
        try {
            pHeader.put(ServerParams.PARAM_PVERSION, ServerParams.P_VERSION);
            pHeader.put(ServerParams.PARAM_REQUEST_ID, String.valueOf(System.currentTimeMillis()));
            pHeader.put(ServerParams.PARAM_UA, LibUtil.getUserAgent(context));
            pHeader.put(ServerParams.PARAM_ADV_ID, LibUtil.getAdvertisingId(context));
            pHeader.put(ServerParams.PARAM_AID, LibUtil.getAndroidId(context));
            pHeader.put(ServerParams.PARAM_LOCAL, LibUtil.getCountry(context));
            pHeader.put(ServerParams.PARAM_LANG, LibUtil.getLauguage(context));
            pHeader.put(ServerParams.PARAM_SYS_NAME, Build.VERSION.RELEASE);
            pHeader.put(ServerParams.PARAM_SYS_CODE, String.valueOf(Build.VERSION.SDK_INT));
            pHeader.put(ServerParams.PARAM_CVERSION, String.valueOf(LibUtil.getAppVersionCode(context)));
            pHeader.put(ServerParams.PARAM_CVNAME, LibUtil.getAppVersion(context));
            pHeader.put(ServerParams.PARAM_PKG_NAME, LibUtil.getPackageName(context));
            pHeader.put(ServerParams.PARAM_SDK_NAME, SdkConfig.SDK_VERSION_NAME);
            pHeader.put(ServerParams.PARAM_SDK_CODE, SdkConfig.SDK_VERSION_CODE);
            pHeader.put(ServerParams.PARAM_NET_TYPE, LibUtil.getNetworkType(context));
            pHeader.put(ServerParams.PARAM_SCREEN_SIZE, LibUtil.getScreenSize(context));
            pHeader.put(ServerParams.PARAM_RAM, String.valueOf(LibUtil.getTotalMemory()));
            pHeader.put(ServerParams.PARAM_IS_TABLET, LibUtil.getDeviceType(context));
            pHeader.put(ServerParams.PARAM_OPERATOR, LibUtil.getCarrier(context));
            pHeader.put(ServerParams.PARAM_ROM, LibUtil.getRomSpace(context));
            pHeader.put(ServerParams.PARAM_CPU, LibUtil.getCPU());
            pHeader.put(ServerParams.PARAM_MODE, LibUtil.getModelName());
            pHeader.put(ServerParams.PARAM_TZ, LibUtil.getTZ(context));
            pHeader.put(ServerParams.PARAM_IS_ROOT, LibUtil.isRootAvailable() ? 1 : 0);
            pHeader.put(ServerParams.PARAM_IS_GP_INSTALLED, LibUtil.isAppExist(context, LibUtil.MARKET_PACKAGE) ? 1 : 0);
            pHeader.put(ServerParams.PARAM_CMD5, LibUtil.getApkMd5(context));
            pHeader.put(ServerParams.PARAM_PLATFORM, "android");
            pHeader.put(ServerParams.PARAM_CDAYS, LibUtil.getInstallDays(context));
            pHeader.put(ServerParams.PARAM_SR, 2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return pHeader;
    }
}
