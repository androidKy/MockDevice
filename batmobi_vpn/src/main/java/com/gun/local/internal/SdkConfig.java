package com.gun.local.internal;

/**
 * Created by de on 2017/1/24.
 */

public class SdkConfig {
    static public boolean ISPROXYMODE = true;
    public static boolean DEBUG = true;
    //连接局域网测试的开关
    public static volatile boolean LOCAL_DEBUG=false;

    public static final String SDK_NAME = "GunVpn_A";
    public static final String SDK_VERSION_NAME = "1.0.8";
    public static final int SDK_VERSION_CODE = 108;


    //连接超时1分钟
    public static final int CONNECTION_TIMEOUT = 60 * 1000;
    //读取超时3分钟
    public static final int READ_TIMEOUT = 180 * 1000;
}
