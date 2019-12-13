package com.gun.local.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.WindowManager;

import com.gun.local.tool.encrypt.CryptTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * description:
 * author: xiaodifu
 * date: 2017/12/7.
 */
public class LibUtil {
    private static String sAppVersion;
    private static int sAppVersionCode = -2;
    private static String sAndroidId = null;
    private static String sImsi = "";
    private static String sCarrier = "";
    private static boolean sCarrierInited;
    private static String sIMEI = "";
    private static String sModel = "";
    public static final int SPTYPE_MOBILE = 0; // 中国移动
    public static final int SPTYPE_UNICOM = 1; // 中国联通
    public static final int SPTYPE_TELECOM = 2; // 中国电信
    private static String sAdvertisingId = null;
    private static String apkMd5;

    public static final String NETWORK_WIFI = "WIFI";
    public static final String NETWORK_2G = "2g";
    public static final String NETWORK_3G = "3g";
    public static final String NETWORK_4G = "4g";
    public static final String NETWORK_UNKOWN = "unknown";
    private static final String TAG="LibUtil";

    // GooglePaly包名
    public static final String MARKET_PACKAGE = "com.android.vending";

    /**
     * 解密
     *
     * @param data
     * @return
     */
    public static String decrypt(String data) {
        if (TextUtils.isEmpty(data)) {
            return data;
        }
        return CryptTool.decrypt(data, "30a161c4b1bde4eea");
    }

    /**
     * 加密
     *
     * @param data
     * @return
     */
    public static String encrypt(String data) {
        if (TextUtils.isEmpty(data)) {
            return data;
        }
        return CryptTool.encrypt(data, "30a161c4b1bde4eea");
    }

    /**
     * 获取存取数据
     *
     * @param context
     * @return
     */
    public static SharedPreferences getSharedPreferences(String name, Context context) {
        if (null != context) {
            SharedPreferences preferences = null;
            if (Build.VERSION.SDK_INT > 10)
                preferences = context.getSharedPreferences(name, Context.MODE_MULTI_PROCESS);
            else
                preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
            return preferences;
        }
        return null;
    }

    /**
     * 检查menifest网络权限
     * @param context
     * @return
     */
    public static boolean manifestInternetPermissionOK(Context context) {
        List permissions;
        try {
            if (!(permissions = Arrays.asList(context.getPackageManager().getPackageInfo(context.getPackageName(), /*4096*/PackageManager.GET_PERMISSIONS).requestedPermissions)).contains("android.permission.INTERNET")) {
                //LogUtil.e("Permission android.permission.INTERNET is missing in the AndroidManifest.xml");
                return false;
            }

            if (!permissions.contains("android.permission.ACCESS_NETWORK_STATE")) {
                //LogUtil.e("Permission android.permission.ACCESS_NETWORK_STATE is missing in the AndroidManifest.xml");
                return false;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    /**
     * 判断网络连接
     * @param context
     * @return
     */
    public static boolean isNetworkOK(Context context) {
        boolean result = false;
        if (context != null) {
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        result = true;
                    }
                }
            } catch (NoSuchFieldError e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static String readServerResponse(HttpURLConnection connection) {
        BufferedReader var2 = null;
        StringBuilder var3 = new StringBuilder();
        InputStreamReader var4 = null;
        boolean var13 = false;

        String var22;
        label136:
        {
            label135:
            {
                try {
                    var13 = true;
                    InputStream var5;
                    if ((var5 = connection.getErrorStream()) == null) {
                        var5 = connection.getInputStream();
                    }

                    var4 = new InputStreamReader(var5);
                    var2 = new BufferedReader(var4);

                    while ((var22 = var2.readLine()) != null) {
                        var3.append(var22).append('\n');
                    }

                    var13 = false;
                    break label135;
                } catch (Throwable var19) {
                    //LogUtil.i(TAG,"Could not read connection response from: " + connection.getURL().toString(), var19);
                    var13 = false;
                } finally {
                    if (var13) {
                        try {
                            if (var2 != null) {
                                var2.close();
                            }

                            if (var4 != null) {
                                var4.close();
                            }
                        } catch (Throwable var15) {
                            ;
                        }

                    }
                }

                try {
                    if (var2 != null) {
                        var2.close();
                    }

                    if (var4 != null) {
                        var4.close();
                    }
                } catch (Throwable var17) {
                    ;
                }
                break label136;
            }

            try {
                var2.close();
                var4.close();
            } catch (Throwable var18) {
                ;
            }
        }

        var22 = var3.toString();

        try {
            new JSONObject(var22);
            return var22;
        } catch (JSONException var16) {
            JSONObject connection1 = new JSONObject();

            try {
                connection1.put("string_response", var22);
                return connection1.toString();
            } catch (JSONException var14) {
                return (new JSONObject()).toString();
            }
        }
    }

    /**
     * 判断是否https
     *
     * @param url
     * @return
     */
    public static boolean isHttps(String url) {
        return url.startsWith("https://");
    }

    /**
     * 替换https
     *
     * @param url
     * @return
     */
    public static String convertHttpsToHttp(String url) {
        if (isHttps(url)) {
            url = "http" + url.substring(5);
        }
        return url;
    }

    /**
     * 解压缩
     *
     * @param str
     * @return
     * @throws Exception
     */
    public static String ungzip(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes("ISO-8859-1"));
            GZIPInputStream gunzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                }
            }
        }
        return out.toString();
    }

    /**
     * 获取User Agent
     *
     * @param context
     * @return
     */
    public static String getUserAgent(Context context) {
        return System.getProperty("http.agent");
    }

    /**
     * 获取GoogleAdvertisingID
     *
     * @param context
     * @return
     */
    public static String getAdvertisingId(Context context) {
        if (null == sAdvertisingId) {
            try {
                if (TextUtils.isEmpty(sAdvertisingId)) {
                    sAdvertisingId = AdvertisingIdClient.getAdvertisingIdInfo(context).getId();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return sAdvertisingId;
    }

    /**
     * 获取android id
     *
     * @param context
     * @return
     */
    public static String getAndroidId(Context context) {
        if (TextUtils.isEmpty(sAndroidId)) {
            sAndroidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        return sAndroidId;
    }

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

    public static String getLauguage(Context context) {
        Locale locale = Locale.getDefault();
        String language = String.format("%s_%s", locale.getLanguage().toLowerCase(), locale.getCountry().toLowerCase());
        return language;
    }

    /**
     * 获取应用版本
     *
     * @param context
     * @return
     */
    public static int getAppVersionCode(Context context) {
        if (sAppVersionCode < 0) {
            initAppVersionCode(context);
        }
        return sAppVersionCode;
    }

    /**
     * @param context
     */
    private static void initAppVersionCode(Context context) {
        if (context == null) {
            sAppVersionCode = 0;
            return;
        }
        try {
            sAppVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            //LogUtil.i("Can't get app version. Exception: " + e.getMessage());
            sAppVersionCode = 0;
        }
    }

    /**
     * 压缩
     *
     * @param bs
     * @return
     * @throws Exception
     */
    public static String gzip(byte[] bs) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GZIPOutputStream gzout = null;
        try {
            gzout = new GZIPOutputStream(bout);
            gzout.write(bs);
            gzout.flush();
        } catch (Exception e) {
            throw e;
        } finally {
            if (gzout != null) {
                try {
                    gzout.close();
                } catch (Exception ex) {
                }
            }
        }
        String result = null;
        if (bout != null) {
            result = bout.toString("ISO-8859-1");
        }
        return result;
    }

    /**
     * 产生32位md5加密字符串
     *
     * @param s
     * @return
     */
    public final static String MD5generator(String s) {
        return to32BitString(s, true, "UTF-8");
    }



    /**
     * 32位MD5加密方法
     * 16位小写加密只需getMd5Value("xxx").substring(8, 24);即可
     *
     * @param sSecret
     * @return
     */
    public static String getMd5Value(String sSecret) {
        try {
            MessageDigest bmd5 = MessageDigest.getInstance("MD5");
            bmd5.update(sSecret.getBytes());
            int i;
            StringBuffer buf = new StringBuffer();
            byte[] b = bmd5.digest();// 加密
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            Logs.e(TAG,"",e);
        }
        return sSecret;
    }
    /**
     * 生成md5
     *
     * @param plainText
     * @param is32
     * @param charset
     * @return
     */
    private static String to32BitString(String plainText, boolean is32, String charset) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (!TextUtils.isEmpty(charset))
                md.update(plainText.getBytes(charset));
            else {
                md.update(plainText.getBytes());
            }
            byte[] b = md.digest();
            String buf = toHexAscii(b, "");
            if (is32) {
                return buf;
            } else {
                return buf.substring(8, 24);
            }

        } catch (Exception e) {
            //LogUtil.i(e.getMessage());
        }
        return "";
    }

    /**
     * @param c 为分隔符
     * @return
     */
    public static String toHexAscii(byte[] bs, String c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bs.length; i++) {
            sb.append(toHexAscii(bs[i]));
            if (i < bs.length - 1)
                sb.append(c);
        }
        return sb.toString();
    }

    public static String toHexAscii(byte b) {
        String s = Integer.toHexString(b);
        return paddingToFixedString(s, '0', 2, true);
    }

    /**
     * 向src的头部或尾部重复添加c字符，添加到finxLen固定长度
     *
     * @param src
     * @param c
     * @param fixLen
     * @param isHead
     * @return
     */
    public static String paddingToFixedString(String src, char c, int fixLen, boolean isHead) {
        String s = src;
        if (s.length() > fixLen) {
            int begin = s.length() - fixLen;
            s = s.substring(begin);
            return s;
        } else if (s.length() == fixLen) {
            return src;
        } else {
            return fill(src, c, fixLen - src.length(), isHead);
        }
    }

    /**
     * @param src    源串
     * @param c      添加的字符
     * @param num    添加c的次数
     * @param ishead 如果为true，在头部添加，否则在尾部添加
     * @return
     */
    public static String fill(String src, char c, int num, boolean ishead) {
        StringBuilder sb = new StringBuilder(src);
        char[] cs = new char[num];
        Arrays.fill(cs, c);
        if (ishead)
            sb.insert(0, cs);
        else {
            sb.append(cs);
        }
        return sb.toString();
    }

    /**
     * 获取应用版本
     *
     * @param context
     * @return
     */
    public static String getAppVersion(Context context) {
        if (sAppVersion == null) {
            initAppVersion(context);
        }
        return sAppVersion;
    }

    private static void initAppVersion(Context context) {
        if (context == null) {
            sAppVersion = "0.0";
            return;
        }
        try {
            sAppVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            //LogUtil.i("Can't get app version. Exception: " + e.getMessage());
            sAppVersion = "0.0";
        }
    }

    /**
     * 获取包名
     *
     * @param context
     * @return
     */
    public static String getPackageName(Context context) {
        return context.getApplicationInfo().packageName;
    }

    /**
     * 网络详细类别，GPRS/EDGE/LTE等
     *
     * @param context
     * @return
     */
    public static String getNetworkType(Context context) {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null && info.isAvailable() && info.getState() == NetworkInfo.State.CONNECTED) {
                    switch (info.getType()) {
                        case ConnectivityManager.TYPE_WIFI:
                            return NETWORK_WIFI;
                        case ConnectivityManager.TYPE_MOBILE:
                            switch (info.getSubtype()) {
                                case TelephonyManager.NETWORK_TYPE_GPRS:
                                    return "GPRS";
                                case TelephonyManager.NETWORK_TYPE_EDGE:
                                    return "EDGE";
                                case TelephonyManager.NETWORK_TYPE_CDMA:
                                    return "CDMA";
                                case TelephonyManager.NETWORK_TYPE_1xRTT:
                                    return "1xRTT";
                                case TelephonyManager.NETWORK_TYPE_IDEN:
                                    return "IDEN";
                                case TelephonyManager.NETWORK_TYPE_UMTS:
                                    return "UMTS";
                                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                                    return "EVDO0";
                                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                                    return "EVDOA";
                                case TelephonyManager.NETWORK_TYPE_HSDPA:
                                    return "HSUPA";
                                case TelephonyManager.NETWORK_TYPE_HSUPA:
                                    return "HSUPA";
                                case TelephonyManager.NETWORK_TYPE_HSPA:
                                    return "HSPA";
                                case 12:// 兼容SDK8
                                    return "EVDOB";
                                case 14:
                                    return "EHRPD";
                                case 15:
                                    return "HSPAP";
                                case 13:
                                    return "LTE";
                                case 16:
                                    return "GSM";
                                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                                    return NETWORK_UNKOWN;
                            }
                        case ConnectivityManager.TYPE_MOBILE_DUN:
                        case ConnectivityManager.TYPE_MOBILE_HIPRI:
                        case ConnectivityManager.TYPE_MOBILE_MMS:
                        case ConnectivityManager.TYPE_MOBILE_SUPL:
                        case ConnectivityManager.TYPE_WIMAX:
                            return NETWORK_UNKOWN;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return NETWORK_UNKOWN;
        }

        return NETWORK_UNKOWN;
    }

    /**
     * 获取屏幕宽高
     *
     * @param context
     * @return
     */
    public static String getScreenSize(Context context) {
        if (null == context) {
            return "";
        }

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();// 手机屏幕的宽度
        int height = wm.getDefaultDisplay().getHeight();// 手机屏幕的高度
        return width + "x" + height;
    }

    /**
     * 获取总运存大小
     *
     * @return
     */
    public static long getTotalMemory() {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        BufferedReader localBufferedReader = null;
        try {
            FileReader localFileReader = new FileReader(str1);
            localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            String regex = "\\s+";
            arrayOfString = str2.split(regex);
            //LogUtil.i(String.format("getTotalMemory:%s,%s,%s", str1, str2, Arrays.toString(arrayOfString)));
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue();// 获得系统总内存，单位是KB
            localBufferedReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != localBufferedReader) {
                try {
                    localBufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return initial_memory / 1024;
    }


    /**
     * 获取设备类型 是平板还是手机
     *
     * @param context
     * @return
     */
    public static int getDeviceType(Context context) {
        boolean isTablet = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
        if (isTablet)
            return 1;
        else
            return 0;
    }

    /**
     * 获取运营商
     *
     * @param context
     * @return
     */
    public static String getCarrier(Context context) {
        if (!sCarrierInited) {
            initCarrier(context);
        }
        return sCarrier;
    }

    private static void initCarrier(Context context) {
        if (context == null) {
            sCarrierInited = true;
            return;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null) {
            sCarrierInited = true;
            return;
        }
        sCarrier = telephonyManager.getNetworkOperator();
        if (TextUtils.isEmpty(sCarrier)) {
            sCarrier = "";
        }
        sCarrierInited = true;
    }

    /**
     * 判断是否是GP URL
     *
     * @param url
     * @return
     */
    public static boolean isGpUrl(String url) {
        if ((url == null) || (url.trim().length() == 0)) {
            return false;
        }

        return (url.startsWith("http://market.android.com")) ||
                (url.startsWith("https://market.android.com")) ||
                (url.startsWith("https://play.google.com")) ||
                (url.startsWith("http://play.google.com")) ||
                (url.startsWith("market://"));
    }

    /**
     * 获取Rom大小
     *
     * @param context
     * @return
     */
    public static String getRomSpace(Context context) {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockCount = stat.getBlockCount();
            long blockSize = stat.getBlockSize();
            return "" + blockCount * blockSize / 1024 / 1024;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取cpu个数
     *
     * @return
     */
    public static int getCPU() {
        try {
            return Runtime.getRuntime().availableProcessors();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 3;
    }

    /**
     * 获取机型
     *
     * @return String 品牌和型号，如 ：HUAWEI G750-T20
     */
    public static String getModelName() {
        if (TextUtils.isEmpty(sModel)) {
            sModel = getSystemProperties("ro.yunos.model"); // YunOS的机型名称
            if (TextUtils.isEmpty(sModel)) {
                sModel = Build.MODEL;
            }
        }
        return sModel;
    }

    public static String getSystemProperties(String prop) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class, String.class);
            String value = (String) method.invoke(null, prop, "");
            return value;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return "";
    }


    /**
     * 获取时区
     *
     * @param context
     * @return
     */
    public static String getTZ(Context context) {
        try {
            TimeZone tz = TimeZone.getDefault();
            return tz.getDisplayName(false, TimeZone.SHORT);
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * @return <code>true</code> if su was found.
     */
    public static boolean isRootAvailable() {
        return (findBinary("su", null)).size() > 0;
    }

    private static List<String> findBinary(final String binaryName, List<String> searchPaths) {
        final List<String> foundPaths = new ArrayList<String>();
        if (searchPaths == null) {
            searchPaths = getPath();
        }
        for (String path : searchPaths) {
            if (!path.endsWith("/")) {
                path += "/";
            }
            if (isFileExist(path + binaryName)) {
                foundPaths.add(path);
            }
        }
        return foundPaths;
    }

    private static List<String> getPath() {
        return Arrays.asList(System.getenv("PATH").split(":"));
    }


    /**
     * 指定路径文件是否存在
     *
     * @param filePath
     * @return
     */
    public static boolean isFileExist(String filePath) {
        boolean result = false;
        try {
            File file = new File(filePath);
            result = file.exists();
            file = null;
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 检测设备中是否有安装指定应用
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isAppExist(final Context context, final String packageName) {
        if (null == context || TextUtils.isEmpty(packageName)) {
            return false;
        }

        boolean result = false;
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SHARED_LIBRARY_FILES);
            result = true;
        } catch (Exception e) {
            result = false;
        }

        return result;
    }

    /**
     * 获取安装天数
     */
    public static long getInstallDays(Context context) {
        if (null == context) {
            return 0l;
        }
        return getInstallDay(context, context.getPackageName());
    }

    /**
     * 获取安装天数
     *
     * @param context
     * @param packageName
     * @return
     */
    public static long getInstallDay(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || null == context) {
            return 0L;
        }
        long ts = getFirstInstallTime(context, packageName);
        if (ts < 1) {
            return 0l;
        }
        return ((System.currentTimeMillis() - ts) / (24 * 60 * 60 * 1000)) + 1;
    }

    /**
     * 获取首次安装时间
     *
     * @param context
     * @param packageName
     * @return
     */
    public static long getFirstInstallTime(Context context, String packageName) {
        try {
            if (!isAppExist(context, packageName)) {
                return 0L;
            }
            PackageManager packageManager = context.getApplicationContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.firstInstallTime;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0L;
    }


    /**
     * 获取宿主apk包的MD5签名16进制大写字符串
     */
    public static String getApkMd5(Context context) {
        if (!TextUtils.isEmpty(apkMd5)) {
            return apkMd5;
        }
        synchronized (LibUtil.class) {
            if (!TextUtils.isEmpty(apkMd5)) {
                return apkMd5;
            }
            try {
                if (context == null) {
                    return "";
                }
                MessageDigest sig = MessageDigest.getInstance("MD5");
                File packageFile = new File(context.getApplicationInfo().sourceDir);
                InputStream is = new FileInputStream(packageFile);
                byte[] buffer = new byte[4096];//每次检验的文件区大小
                long toRead = packageFile.length();
                long soFar = 0;
                boolean interrupted;
                while (soFar < toRead) {
                    interrupted = Thread.interrupted();
                    if (interrupted) break;
                    int read = is.read(buffer);
                    soFar += read;
                    sig.update(buffer, 0, read);
                }
                byte[] digest = sig.digest();
                String digestStr = bytesToHexString(digest);
                if (digestStr != null) {
                    return apkMd5 = digestStr.toUpperCase();
                }
            } catch (Exception e) {
                return "";
            }
            return "";
        }
    }


    /**
     * MD5值移位转换,byte数组转成16进制字符串
     */
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        int i = 0;
        while (i < src.length) {
            int v;
            String hv;
            v = (src[i] >> 4) & 0x0F;
            hv = Integer.toHexString(v);
            stringBuilder.append(hv);
            v = src[i] & 0x0F;
            hv = Integer.toHexString(v);
            stringBuilder.append(hv);
            i++;
        }
        return stringBuilder.toString();
    }
}
