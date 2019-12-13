package com.gun.local.proxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Proxy;

/**
 * description:
 * author: kyXiao
 * date: 2019/4/2
 */
public class AutoProxyMain {

    private Context mContext;
    private ProxyListener mProxyListener;

    public AutoProxyMain(Context context) {
        this.mContext = context;
    }

    public AutoProxyMain setProxyListener(ProxyListener proxyListener) {
        mProxyListener = proxyListener;
        return this;
    }

    /**
     * 先开启socket,然后通过socket命令
     *
     * @param country 这个是国家的缩写，比如印度尼西亚(Indonesia，ID)
     * @param port
     */
    public void connectVPN(final String country, final int port) {
        sendSocket(country, port, ProxyOperate.CONNECT_PROXY, new SocketManager.MsgListener() {
            @Override
            public void onSentSucceed() {
                checkIp();
            }

            @Override
            public void onSendFailed() {
                if (mProxyListener != null)
                    mProxyListener.onFailed();
            }
        });

    }

    public void disconnectVPN(String country, int port) {
        sendSocket(country, port, ProxyOperate.FREE_PROXY, new SocketManager.MsgListener() {
            @Override
            public void onSentSucceed() {
                checkIp();
            }

            @Override
            public void onSendFailed() {
                if (mProxyListener != null)
                    mProxyListener.onFailed();
            }
        });
    }


    private void checkIp() {
        CheckIpUtil.checkIp(new CheckIpUtil.CheckIpListener() {
            @Override
            public void onCheckIpResult(String resultCountry, String resultIp) {
                Log.i("Process", "onCheckIpResult: resultCountry = " + resultCountry + " resultIp = " + resultIp);
                SharedPreferences sharedPreferences = mContext.getSharedPreferences("auto_proxy", Context.MODE_PRIVATE);
                String savedIP = sharedPreferences.getString("curIP", "");
                if (!TextUtils.isEmpty(resultIp) && !savedIP.equals(resultIp))
                    mProxyListener.onSuccess();
                else mProxyListener.onFailed();
            }
        });
    }

    private void sendSocket(String country, int port, ProxyOperate proxyOperate, SocketManager.MsgListener msgListener) {
        SocketManager socketManager = new SocketManager(country, port);
        socketManager.sendMsg(proxyOperate, msgListener);
    }


    public interface ProxyListener {
        void onSuccess();

        void onFailed();
    }

}
