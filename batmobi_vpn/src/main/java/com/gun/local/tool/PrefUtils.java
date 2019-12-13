package com.gun.local.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * description:SharedPreferences工具类
 * author: xiaodifu
 * date: 2017/8/1.
 */
public class PrefUtils {
    private static final String PREF_UPDATE_COMMON = "pref_update_common";
    private static final String PREF_UPDATE_REQ_DATA = "pref_update_req_data";

    public static SharedPreferences getCommonSP(Context context) {
        return getSP(PREF_UPDATE_COMMON, context);
    }

    public static SharedPreferences getUpdateDataSDP(Context context) {
        return getSP(PREF_UPDATE_REQ_DATA, context);
    }

    /**
     * 保存数据
     *
     * @param context
     * @param pref
     * @param key
     * @param object
     */
    public static void setSP(Context context, String pref, String key, Object object) {
        if (object == null) return;
        String type = object.getClass().getSimpleName();
        SharedPreferences.Editor editor = getSP(pref, context).edit();
        if ("String".equals(type)) {
            editor.putString(key, (String) object);
        } else if ("Integer".equals(type)) {
            editor.putInt(key, (Integer) object);
        } else if ("Boolean".equals(type)) {
            editor.putBoolean(key, (Boolean) object);
        } else if ("Float".equals(type)) {
            editor.putFloat(key, (Float) object);
        } else if ("Long".equals(type)) {
            editor.putLong(key, (Long) object);
        }
        commit(editor);
    }

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param context
     * @param key
     * @param object
     */
    public static void setCommonSP(Context context, String key, Object object) {
        setSP(context, PREF_UPDATE_COMMON, key, object);
    }

    /**
     * 设置请求数据
     *
     * @param context
     * @param key
     * @param object
     */
    public static void setUpdateDataSP(Context context, String key, Object object) {
        setSP(context, PREF_UPDATE_REQ_DATA, key, object);
    }

    /**
     * 获取存取数据
     *
     * @param context
     * @return
     */
    public static SharedPreferences getSP(String name, Context context) {
        if (null != context) {
            SharedPreferences preferences = null;
            if (Build.VERSION.SDK_INT > 10)
                preferences = context.getSharedPreferences(name, 0x0004);
            else
                preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            return preferences;
        }
        return null;
    }


    /**
     * 清理
     *
     * @param context
     */
    public static void clear(Context context) {
        SharedPreferences.Editor editor = getUpdateDataSDP(context).edit();
        editor.clear();
        commit(editor);
    }

    /**
     * /**
     * 提交存储
     *
     * @param editor
     */
    public static void commit(SharedPreferences.Editor editor) {
        editor.apply();
    }

    public static class PrefKeys {
        public static final String PROXY_ADDRESS = "_proxy_address";
        public static final String PROXY_PORT = "_proxy_port";
        public static final String KEY_APPKEY = "_key_appkey";
        public static final String KEY_COUNTRY = "_key_country";
        public static final String KEY_RECONNECT_B = "_key_reconnect_b";
        public static final String KEY_EXCEPTION_RECONNECT_B = "_key_exception_reconnect_b";
    }


}
