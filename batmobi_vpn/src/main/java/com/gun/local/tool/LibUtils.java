package com.gun.local.tool;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.util.Locale;

/**
 * description:工具类
 * author: diff
 * date: 2017/12/27.
 */
public class LibUtils {

    /**
     * 获取国家
     *
     * @param context
     * @return
     */
    public static String getCountry(Context context) {
        String ret = "";

        try {
            TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager != null) {
                ret = telManager.getSimCountryIso().toUpperCase();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(ret)) {
            ret = Locale.getDefault().getCountry().toString().toUpperCase();
        }

        return ret;
    }
}
