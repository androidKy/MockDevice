package com.gun.local.proxy;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * description:
 * author: kyXiao
 * date: 2019/4/2
 */
public class CheckIpUtil {
    private static final String TAG = "CheckIpUtil";

    /**
     * 检查IP是否已切换
     */
    public static void checkIp(CheckIpListener checkIpListener) {
        checkRunOnAsyncThread();
        try {
            URL url = new URL("https://ip.cn/");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15 * 1000);
            urlConnection.setReadTimeout(15 * 1000);
            urlConnection.connect();

            if (urlConnection.getResponseCode() == 200) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                char[] chars = new char[1024];
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                Log.i(TAG, "checkIp: result\n" + sb.toString());
                parseHtml(sb.toString(), checkIpListener);
            } else {
                if (checkIpListener != null)
                    checkIpListener.onCheckIpResult("", "");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (checkIpListener != null)
                checkIpListener.onCheckIpResult("", "");
        }
    }

    private static void parseHtml(String html, CheckIpListener checkIpListener) {
        try {
            if (!TextUtils.isEmpty(html)) {
                Document document = Jsoup.parse(html);
                Elements element = document.body().getElementsByClass("container-fluid");
                Elements resultElement = element.get(0).getElementById("result").getElementsByClass("well");
                Elements pElements = resultElement.get(0).getElementsByTag("p");
                Element codeElement = pElements.get(0).getElementsByTag("code").get(0);
                String curIP = codeElement.text();
                Element countryElement = pElements.get(2);
                String countryContent = countryElement.text();
                String[] strs = countryContent.split(",");
                String curCountry = "";
                if (strs.length > 2)
                    curCountry = strs[2];
                Log.i(TAG, "parseHtml: curIp = " + curIP + " curCountry=" + curCountry);
                if (checkIpListener != null)
                    checkIpListener.onCheckIpResult(curCountry, curIP);
            } else {
                Log.i(TAG, "parseHtml: html is empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (checkIpListener != null)
                checkIpListener.onCheckIpResult("", "");
        }
    }

    private static void checkRunOnAsyncThread() {
        if (isMainThread()) {
            throw new IllegalStateException("can't do this on main thread!");
        }
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public interface CheckIpListener {
        void onCheckIpResult(String country, String ip);
    }
}
