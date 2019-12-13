package com.mock.device;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mock.device.bean.DeviceParamsBean;
import com.proxy.droid.ProxyManager;
import com.proxy.droid.core.ProxyCore;
import com.proxy.droid.core.ProxyCoreImpl;
import com.proxy.droid.bean.ProxyParamsBean;
import com.proxy.droid.core.ProxyStatusListener;
import com.safframework.log.L;
import com.utils.common.CMDUtil;
import com.utils.common.DevicesUtil;
import com.utils.common.PermissionUtils;
import com.utils.common.SPUtils;
import com.utils.common.ThreadUtils;
import com.utils.common.ToastUtils;
import com.utils.common.pdd_api.ApiManager;
import com.utils.common.pdd_api.DataListener;
import com.vm.shadowsocks.LocalVpnManager;
import com.vm.shadowsocks.bean.ProxyIPBean;
import com.vm.shadowsocks.core.AppInfo;
import com.vm.shadowsocks.core.ProxyConfig;
import com.vm.shadowsocks.network.ProxyApiManager;
import com.vm.shadowsocks.network.ProxyDataListener;
import com.xposed.device.hook.PkgConstant;
import com.xposed.device.hook.sp.DeviceParams;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static com.vm.shadowsocks.LocalVpnManager.START_VPN_SERVICE_REQUEST_CODE;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView mTvServerImei;
    private TextView mTvRealImei;
    private TextView mTvImeiTip;
    private TextView mTvProxyTip;
    private List<AppInfo> mProxyAppList = new ArrayList<>();
    private boolean mIsOpenedDns = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requestPermissionDynamic();
        ProxyConfig.Instance.globalMode = true;
        addProxyApp();

        new ProxyManager.Builder()
                .setContext(this)
                .build()
                .init();

    }

    @Override
    protected void onResume() {
        super.onResume();

        //new ProxyCore(this.getApplication()).init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new ProxyManager().stopProxy();
    }

    private void initView() {
        Button mBtMockDevice = findViewById(R.id.btMockDevice);
        mTvRealImei = findViewById(R.id.tvRealImei);
        mTvServerImei = findViewById(R.id.tvServerImei);
        mTvProxyTip = findViewById(R.id.tvProxyTip);
        mTvImeiTip = findViewById(R.id.tvImeiTip);
        Button btClearInfo = findViewById(R.id.btClearInfo);

        mBtMockDevice.setOnClickListener(this);
        btClearInfo.setOnClickListener(this);
        findViewById(R.id.start_proxy).setOnClickListener(this);
        findViewById(R.id.stop_proxy).setOnClickListener(this);
        findViewById(R.id.open_dns).setOnClickListener(this);
        findViewById(R.id.close_dns).setOnClickListener(this);
    }

    private void addProxyApp() {
        mProxyAppList.clear();
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packageInfoList = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packageInfoList = packageManager.getInstalledPackages(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        } else {
            packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        }

        for (int i = 0; i < packageInfoList.size(); i++) {
            PackageInfo packageInfo = packageInfoList.get(i);
            String appName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
            String packageName = packageInfo.packageName;

            AppInfo appInfo = new AppInfo();
            appInfo.setAppLabel(appName);
            appInfo.setPkgName(packageName);
            //AppProxyManager.Instance.mlistAppInfo.add(appInfo);

            if (packageName.equals(getPackageName()) || packageName.equals("com.android.browser")
            ) {
                mProxyAppList.add(appInfo);
            }
        }
    }

    private void requestPermissionDynamic() {
        List<String> permissonList = new ArrayList<>();
        permissonList.add(Manifest.permission.READ_PHONE_STATE);
        permissonList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissonList.add(Manifest.permission.INTERNET);
        permissonList.add(Manifest.permission.READ_EXTERNAL_STORAGE);

        PermissionUtils.permission(permissonList)
                .callback(new PermissionUtils.FullCallback() {
                    @Override
                    public void onGranted(List<String> permissionsGranted) {
                        String realIMEI = SPUtils.getInstance(MainActivity.this, PkgConstant.SP_DEVICE_PARAMS)
                                .getString(Constant.KEY_REAL_IMEI, "");
                        if (TextUtils.isEmpty(realIMEI)) {
                            realIMEI = DevicesUtil.getIMEI(MainActivity.this);
                            L.i("真实IMEI：" + realIMEI);

                            SPUtils.getInstance(MainActivity.this, PkgConstant.SP_DEVICE_PARAMS)
                                    .put(Constant.KEY_REAL_IMEI, realIMEI);
                        }
                        mTvRealImei.setText(realIMEI);
                    }

                    @Override
                    public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                        System.exit(0);
                    }
                })
                .request();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btMockDevice:
                mTvImeiTip.setText("未修改IMEI");
                getDeviceParams();
                break;
            case R.id.start_proxy:
                //connectProxy();
                startProxy();
                break;
            case R.id.stop_proxy:
                new ProxyCore(this).stopProxy();
                break;
            case R.id.open_dns:
                mIsOpenedDns = true;
                break;
            case R.id.close_dns:
                mIsOpenedDns = false;
                break;
            case R.id.btClearInfo:
                clearData();
                break;
        }
       /* if (view.getId() == R.id.btMockDevice) {
            mTvProxyTip.setText("代理未连接");
            mTvImeiTip.setText("未修改IMEI");
            //1、从服务器获取参数
            //2、连接代理
            getDeviceParams();
            connectProxy();
        } else if (view.getId() == R.id.btClearInfo) {
            // connectIp138();

        }*/
    }

    private void clearData(){
        ThreadUtils.executeByCached(new ThreadUtils.Task<Boolean>() {
            @Override
            public Boolean doInBackground() throws Throwable {
                String clearPackages = //"pm clear " + Constant.QQ_FULL_PKG + ";" +
                        //"pm clear " + Constant.QQ_TIM_PKG + ";" +
                        //"pm clear " + Constant.WECHAT_PKG + ";" +
                        "pm clear com.android.browser;";// +
                        //"pm clear com.xingen.app;";
                String result = new CMDUtil().execCmd(clearPackages);
                return result.contains("Success");
            }

            @Override
            public void onSuccess(Boolean result) {
                mTvImeiTip.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.Companion.showToast(MainActivity.this, "QQ,微信,浏览器的信息已清除");
                    }
                }, 3000);
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onFail(Throwable t) {

            }
        });
    }


    private void setImeiView(String imei, String tip) {
        mTvImeiTip.setText(tip);
        mTvServerImei.setText(imei);
    }

    /**
     * 获取设备参数
     */
    private void getDeviceParams() {
        new ApiManager()
                .setDataListener(new DataListener() {
                    @Override
                    public void onSucceed(@NotNull String result) {
                        L.i("获取设备信息：" + result);
                        saveDeviceParams(result);
                    }

                    @Override
                    public void onFailed(@NotNull String errorMsg) {
                        L.i("获取设备信息失败：" + errorMsg);
                        //mTvServerImei.setText("修改IMEI失败：" + errorMsg);
                        setImeiView("", "修改IMEI失败:" + errorMsg);
                    }
                })
                .getDeviceParams();
    }

    private void saveDeviceParams(String result) {
        DeviceParamsBean deviceParamsBean = new Gson().fromJson(result, DeviceParamsBean.class);
        if (deviceParamsBean.getCode() == 200) {
            SPUtils spUtils = SPUtils.getInstance(MainActivity.this, PkgConstant.SP_DEVICE_PARAMS);
            DeviceParamsBean.DeviceBean deviceBean = deviceParamsBean.getData();
            spUtils.put(DeviceParams.Companion.getIMEI_KEY(), deviceBean.getImei());
            spUtils.put(DeviceParams.Companion.getIMSI_KEY(), deviceBean.getImsi());
            spUtils.put(DeviceParams.Companion.getBLUTOOTH_KEY(), deviceBean.getBluetooth());
            spUtils.put(DeviceParams.Companion.getBRAND_KEY(), deviceBean.getBrand());
            spUtils.put(DeviceParams.Companion.getMAC_KEY(), deviceBean.getMac());
            spUtils.put(DeviceParams.Companion.getSDK_KEY(), deviceBean.getSn());
            spUtils.put(DeviceParams.Companion.getMODEL_KEY(), deviceBean.getModel());
            spUtils.put(DeviceParams.Companion.getSYSTEM_KEY(), deviceBean.getSystem());
            spUtils.put(DeviceParams.Companion.getUSER_AGENT_KEY(), deviceBean.getUseragent());

            setImeiView(deviceBean.getImei(), "IMEI已修改");
        }
    }

    private void startProxy() {
        String imei = SPUtils.getInstance(MainActivity.this, PkgConstant.SP_DEVICE_PARAMS)
                .getString(Constant.KEY_REAL_IMEI, "");
        L.i("connectProxy 真实imei：" + imei);
        String cityName = "重庆市";
        new ProxyManager.Builder()
                .setContext(this)
                .setCityName("")
                .setImei(imei)
                .setProxyStatusListener(new ProxyStatusListener() {
                    @Override
                    public void onProxyStatus(boolean status, String msg) {
                        L.i("代理状态：" + status + " 日志：" + msg);
                        ToastUtils.Companion.showToast(getApplication(), "代理状态：" + status + " 日志：" + msg);
                        if (status)
                            mTvProxyTip.setText("代理已连接");
                        else mTvProxyTip.setText("代理连接失败");
                    }
                })
                .build()
                .startProxy();
    }

    /**
     * 连接代理
     */
    private void connectProxy() {
        String imei = SPUtils.getInstance(MainActivity.this, PkgConstant.SP_DEVICE_PARAMS)
                .getString(Constant.KEY_REAL_IMEI, "");
        L.i("connectProxy 真实imei：" + imei);
        String cityName = "茂名市";
        final Activity activity = MainActivity.this;
        new ProxyApiManager(cityName, imei)
                .requestPortByClosePort(new ProxyDataListener() {
                    @Override
                    public void onResponProxyData(@org.jetbrains.annotations.Nullable ProxyIPBean proxyIPBean) {
                        if (proxyIPBean == null) {
                            //responError("请求获取代理数据失败");
                            L.i("请求获取代理数据失败");
                        } else {
                            ProxyParamsBean proxyParamsBean = new ProxyParamsBean();
                            /*proxyParamsBean.setHost("61.154.46.185");
                            proxyParamsBean.setPort("4253");
                            proxyParamsBean.setAuth(false);*/

                            proxyParamsBean.setHost(proxyIPBean.getData().getDomain());
                            proxyParamsBean.setPort(String.valueOf(proxyIPBean.getData().getPort().get(0)));
                            proxyParamsBean.setAuth(true);
                            proxyParamsBean.setUser(proxyIPBean.getData().getAuthuser());
                            proxyParamsBean.setPassword(proxyIPBean.getData().getAuthpass());
                            proxyParamsBean.setAutoSetProxy(true);
                            proxyParamsBean.setAutoConnect(false);
                            proxyParamsBean.setProxyType("socks5");
                            proxyParamsBean.setDNSProxy(mIsOpenedDns);

                            new ProxyCore(MainActivity.this.getApplicationContext())
                                    .setProxyParams(proxyParamsBean)
                                    .setProxyStatusListener(new ProxyStatusListener() {
                                        @Override
                                        public void onProxyStatus(boolean status, String msg) {
                                            String statusStr = status ? "开启" : "未开始";
                                            Log.d("ProxyCore", "代理状态：" + statusStr);
                                        }
                                    })
                                    .startProxy();
                           /* String proxyUrl = "http://(" + proxyIPBean.getData().getAuthuser() + ":"
                                    + proxyIPBean.getData().getAuthpass() + ")@" +
                                    proxyIPBean.getData().getDomain() + ":" + proxyIPBean.getData().getPort().get(0);
                            L.i("proxyUrl:" + proxyUrl);
                            Uri uri = Uri.parse(proxyUrl);
                            String userInfoString = uri.getUserInfo();
                            if (userInfoString != null) {
                                String[] userStrings = userInfoString.split(":");
                                String userName = userStrings[0];
                                if (userStrings.length >= 2) {
                                    String userPsw = userStrings[1];
                                }
                            }
                            String host = uri.getHost();
                            int port = uri.getPort();
                            L.i("host:"+host+" port:"+port);

                            GunLib.setProxy(activity,host,port,"");
                            GunLib.start(activity);

                            //responSucceed(proxyIPBean);
                           LocalVpnManager localVpnManager = LocalVpnManager.getInstance();
                            List<Integer> portList = proxyIPBean.getData().getPort();
                            int port = 0;
                            if (portList.size() > 0) {
                                port = portList.get(0);
                            }

                            localVpnManager.initData(MainActivity.this, proxyIPBean.getData().getAuthuser(),
                                    proxyIPBean.getData().getAuthpass(), proxyIPBean.getData().getDomain(),
                                    String.valueOf(port)
                            );
                            localVpnManager.startVpnService(activity);*/
                        }
                    }

                    @Override
                    public void onFailed(@NotNull String failedMsg) {
                        //responError(failedMsg);
                        L.i("请求获取代理失败：" + failedMsg);
                    }
                });
        /*new ProxyCore()
                .setProxyApps(mProxyAppList)
                .setProxyStatusListener(new LocalVpnService.onStatusChangedListener() {
                    @Override
                    public void onStatusChanged(String status, final Boolean isRunning) {
                        L.i("代理状态变化：" + status);
                        mTvImeiTip.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (isRunning) {
                                    mTvProxyTip.setText("代理已连接");

                                }
                            }
                        }, 3000);
                    }

                    @Override
                    public void onLogReceived(String logString) {
                        L.i("代理日志：" + logString);
                    }
                })
                .setProxyDataListener(new ProxyDataListener() {
                    @Override
                    public void onFailed(@NotNull String failedMsg) {
                        L.i("请求代理数据失败：" + failedMsg);
                        mTvProxyTip.setText(failedMsg);
                    }

                    @Override
                    public void onResponProxyData(@org.jetbrains.annotations.Nullable ProxyIPBean proxyIPBean) {
                        L.i("请求代理数据成功：" + proxyIPBean.toString());
                    }
                })
                .startProxy(this, "重庆市", imei);*/
    }

    /**
     * 连接138
     */
    private void connectIp138() {
        ThreadUtils.executeByCached(new ThreadUtils.Task<Boolean>() {

            @Override
            public Boolean doInBackground() throws Throwable {
                System.setProperty("http.keepAlive", "false");
                URL url = new URL("http://200019.ip138.com/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setReadTimeout(10 * 1000);
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    InputStream inputStream = conn.getInputStream();
                    int length = 0;
                    byte[] bytes = new byte[1024];
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    L.i("请求138结果：" + stringBuilder.toString());
                }
                return null;
            }

            @Override
            public void onSuccess(Boolean result) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onFail(Throwable t) {

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            try {
                L.i("VPN启动回调的Activity");
                if (resultCode == RESULT_OK) {
                    LocalVpnManager localVpnManager = LocalVpnManager.getInstance();
                    localVpnManager.startVpnService(this);
                } else {
                    //log("onActivityResult", "resultCode != RESULT_OK")
                    //onLogReceived("canceled.")
                    //EventBus.getDefault().postSticky(PostModel(PostCode.DisConnect_VPN))
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
