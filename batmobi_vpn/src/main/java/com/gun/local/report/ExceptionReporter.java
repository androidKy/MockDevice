package com.gun.local.report;

import com.gun.local.GunLib;
import com.gun.local.internal.SdkConfig;
import com.gun.local.tool.LibUtil;
import com.gun.local.tool.Logs;

import java.util.HashMap;
import java.util.Map;

/**
 * description:异常上报
 * author: diff
 * date: 2018/1/15.
 */
public class ExceptionReporter extends BaseReporter {
    private static final String TAG = "ExceptionReporter";
    private String exceptionMsg;
    private String serverDomain;

    public ExceptionReporter(String exceptionMsg, String serverDomain) {
        this.exceptionMsg = exceptionMsg;
        this.serverDomain = serverDomain;
    }

    @Override
    public String getServerAddress() {
        String url = String.format("http://%s%s/v1/endian/excep/info", serverDomain, SdkConfig.LOCAL_DEBUG ? ":8080" : "");
        Logs.i(TAG, "url:" + url);
        return url;
    }

    @Override
    Map<String, Object> getExtraParams() {
        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("ud", LibUtil.getAndroidId(GunLib.getContext()));
        extraParams.put("isa", 1);
        extraParams.put("ex", exceptionMsg);
        return extraParams;
    }
}
