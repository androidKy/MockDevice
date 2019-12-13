package com.gun.local.tool;

import android.net.Uri;
import android.text.TextUtils;

import com.gun.local.GunLib;
import com.gun.local.internal.SdkConfig;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLException;

public class HttpPostUtil {
    private static final String TAG = "HttpPostUtil";
    private int MAX_RETRY_COUNT = 3;
    private int RETRY_INTERVAL = 1 * 1000;
    public int retry_count = 1;
    private String isZip;

    /**
     * 不添加参数的构造方法
     */
    public HttpPostUtil() {
    }

    public HttpPostUtil(String isZip) {
        this.isZip = isZip;
    }

    /**
     * 添加重连次数的构造方法
     *
     * @param retryCount
     */
    public HttpPostUtil(int retryCount) {
        MAX_RETRY_COUNT = retryCount <= MAX_RETRY_COUNT ? retryCount : MAX_RETRY_COUNT;
    }

    public String connToUrl(String urlString) {
        String resultData = "";
        for (int i = 0; i < retry_count; i++) {
            resultData = connToUrlForData(urlString);
            if (!TextUtils.isEmpty(resultData)) {
                return resultData;
            }
            try {
                Thread.sleep(i * RETRY_INTERVAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Logs.i(TAG, "[request]:" + urlString + ",resultData=" + resultData);
        return resultData;
    }

    /**
     * 更新 hogan 修改数据流读取和关闭读取流
     *
     * @param urlString
     * @return
     */
    private String connToUrlForData(String urlString) {
        HttpURLConnection conn = null; // 连接对象
        InputStream is = null;
        BufferedReader bufferReader = null;
        InputStreamReader isr = null;
        try {
            URL url = new URL(urlString); // URL对象
            conn = (HttpURLConnection) url.openConnection(); // 使用URL打开一个链接
            conn.setUseCaches(false); // 不使用缓冲
            conn.setReadTimeout(SdkConfig.READ_TIMEOUT);
            conn.setConnectTimeout(SdkConfig.CONNECTION_TIMEOUT);
            conn.setRequestMethod("GET"); // 使用get请求
            is = conn.getInputStream(); // 获取输入流，此时才真正建立链接
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            isr = new InputStreamReader(is);
            bufferReader = new BufferedReader(isr);
            String inputLine = null;
            StringBuffer buffer = new StringBuffer();
            while ((inputLine = bufferReader.readLine()) != null) {
                buffer.append(inputLine);
            }
            return buffer.toString();
        } catch (SSLException ex) {
            ex.printStackTrace();
            if (LibUtil.isHttps(urlString)) {
                urlString = LibUtil.convertHttpsToHttp(urlString);
                connToUrl(urlString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            closeSilent(bufferReader);
            closeSilent(isr);
            closeSilent(is);
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
     * 关闭IO
     *
     * @param closeable
     */
    public void closeSilent(Closeable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取post数据
     *
     * @param url
     * @param nvps
     * @param userAgent
     * @return
     */
    public String post(String url, Map<String, String> nvps, String userAgent) {
        if (nvps.isEmpty()){
            return null;
        }
        String param = convertMapToParam(nvps);
        return post(url, param, userAgent, 0, 0);
    }

    public String post(String url, Map<String, String> nvps, String userAgent, int readTimeoutMillis, int connectTimeoutMillis) {
        String param = convertMapToParam(nvps);
        return post(url, param, userAgent, readTimeoutMillis, connectTimeoutMillis);
    }

    /**
     * 发送数据
     *
     * @param url
     * @param data
     * @param userAgent
     * @return
     */
    public String post(String url, String data, String userAgent, int readTimeoutMillis, int connectTimeoutMillis) {
        String result = "";
        for (int i = 0; i < retry_count; i++) {
            result = doPost(url, data, userAgent, readTimeoutMillis, connectTimeoutMillis);
            if (!TextUtils.isEmpty(result)) {
                return result;
            }
            try {
                Thread.sleep(i * RETRY_INTERVAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public String post(String url, String data, String userAgent) {
        return post(url, data, userAgent, 0, 0);
    }

    private String doPost(String url, String data, String userAgent, int readTimeoutMillis, int connectTimeoutMillis) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        HttpURLConnection conn = null;
        Logs.i(TAG, new StringBuilder("[url]:").append(url).append("[data]:").append(data).toString());
        try {
            Uri uri = Uri.parse(url);
            if (TextUtils.isEmpty(isZip)) {
                isZip = uri.getQueryParameter("isz");
            }
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            conn = (HttpURLConnection) realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            if (TextUtils.isEmpty(userAgent)) {
                userAgent = LibUtil.getUserAgent(GunLib.getContext());
            }
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setUseCaches(false);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            int rTimeout = readTimeoutMillis > 0 ? readTimeoutMillis : SdkConfig.READ_TIMEOUT;
            int cTimeout = connectTimeoutMillis > 0 ? connectTimeoutMillis : SdkConfig.CONNECTION_TIMEOUT;
            conn.setReadTimeout(rTimeout);
            conn.setConnectTimeout(cTimeout);
            /*if (data != null && !data.trim().equals("")) {
                // 获取URLConnection对象对应的输出流
                out = new PrintWriter(conn.getOutputStream());
                // 发送请求参数
                out.print(data);
                // flush输出流的缓冲
                out.flush();
            }
            // 定义BufferedReader输入流来读取URL的响应
//			in = new BufferedReader(
//					new InputStreamReader(conn.getInputStream()));
//			String line;
//			while ((line = in.readLine()) != null) {
//				result += line;
//			}
            byte[] buffer = new byte[1024];
            int len = -1;
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            while ((len = conn.getInputStream().read(buffer)) != -1) {
                byteOut.write(buffer, 0, len);
            }
            byteOut.flush();
            byteOut.close();
            result = byteOut.toString();*/

            OutputStreamWriter outputStreamWriter = null;
            boolean var26 = false;

            try {
                var26 = true;
                (outputStreamWriter = new OutputStreamWriter(conn.getOutputStream(), "UTF-8")).write(data);
                var26 = false;
            } finally {
                if (var26) {
                    if (outputStreamWriter != null) {
                        outputStreamWriter.close();
                    }

                }
            }
            outputStreamWriter.close();

            int respCode = conn.getResponseCode();
            result = LibUtil.readServerResponse(conn);

            Logs.i(TAG, "respCode:" + respCode + ",res:" + result);

            if ("1".equals(isZip)) {
                result = LibUtil.ungzip(result);
            }
        } catch (SSLException ex) {
            ex.printStackTrace();
            if (LibUtil.isHttps(url)) {
                url = LibUtil.convertHttpsToHttp(url);
                return doPost(url, data, userAgent, readTimeoutMillis, connectTimeoutMillis);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (LibUtil.isHttps(url)) {
                url = LibUtil.convertHttpsToHttp(url);
                return doPost(url, data, userAgent, readTimeoutMillis, connectTimeoutMillis);
            }
        } finally { // 使用finally块来关闭输出流、输入流
            closeSilent(out);
            closeSilent(in);
            if (null != conn) {
                conn.disconnect();
            }
        }
        return result;
    }

    /**
     * 转化成字符串
     *
     * @param nvps
     * @return
     */
    private static String convertMapToParam(Map<String, String> nvps) {
        if (null == nvps || nvps.isEmpty()) {
            return "";
        }
        Uri.Builder builder = new Uri.Builder();
        for (Map.Entry<String, String> entry : nvps.entrySet()) {
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        String params = builder.toString().substring(1);
        Logs.i(TAG, String.format("param:%s", params));
        return params;
    }
}
