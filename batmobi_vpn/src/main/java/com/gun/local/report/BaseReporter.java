package com.gun.local.report;

import com.gun.local.GunLib;
import com.gun.local.internal.SdkConfig;
import com.gun.local.tool.HttpPostUtil;
import com.gun.local.tool.LibUtil;
import com.gun.local.tool.Logs;
import com.gun.local.tool.ThreadPool;
import com.gun.local.tool.encrypt.CryptTool;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * description:
 * author: diff
 * date: 2018/1/15.
 */
public abstract class BaseReporter implements IReporter {
    private static final String TAG = "BaseReporter";

    abstract Map<String, Object> getExtraParams();

    @Override
    public void report() {
        ThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = new HttpPostUtil().post(getServerAddress(), getParams(), null);
                    Logs.i(TAG, "result=" + result);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public Map<String, String> getParams() {
        Map<String, String> params = new HashMap<String, String>();
        try {
            JSONObject postData = new JSONObject();
            JSONObject pheadJson = ServerParams.buildHeader(GunLib.getContext());
            postData.put("phead", pheadJson);
            //传入额外参数
            if (getExtraParams() != null) {
                for (Map.Entry<String, Object> entry : getExtraParams().entrySet()) {
                    postData.put(entry.getKey(), entry.getValue());
                }
            }
            String data = postData.toString();
            data = CryptTool.encrypt(data, "30a161c4b1bde4eea");
            if (data != null) {
                data = LibUtil.gzip(data.getBytes());
            }
            params.put("data", data);
            if (SdkConfig.DEBUG) {
                params.put("shandle", "0");
            }
            params.put("pkey", "zz2017");
            params.put("sign", LibUtil.MD5generator("30a161c4b1bde4eea" + postData.toString() + "30a161c4b1bde4eea"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }
}
