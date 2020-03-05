package com.task.cn.jbean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class DeviceInfoBean extends RealmObject implements Serializable {
    /**
     * id : 1004
     * account_ids : [{"account_id":1001},{"account_id":1002}]
     * android.bluetooth.BluetoothAdapter.MacAddress : 00:12:47:29:09:1E
     * android.bluetooth.BluetoothAdapter.name : LeMobile
     * android.content.res.display.dpi :
     * android.content.res.language :
     * android.location.Location.getLatitude : 0.0
     * android.location.Location.getLongitude : 0.0
     * android.net.NetworkInfo.getType : 3
     * android.net.wifi.WifiInfo.NetIpAddr :
     * android.net.wifi.WifiInfo.getBSSID : 00:18:32:76:BE:E5
     * android.net.wifi.WifiInfo.getMacAddress : 00:1E:E1:51:41:16
     * android.net.wifi.WifiInfo.getSSID : HUAWEI_T4DDGD3
     * android.os.Build.ID : MDA89D
     * android.os.Build.VERSION.RELEASE : 6.0
     * android.os.Build.VERSION.SDK : 23
     * android.os.Build.description : LeMobile-user 6.0 MDA89D ac3ef9367eb5 release-keys
     * android.os.Build.fingerprlong : LeMobile/LeMobile/LeMobile:6.0/MDA89D/ac3ef9367eb5:user/release-keys
     * android.os.Build.ro.product.manufacturer : LeMobile
     * android.os.Build.ro.product.model : Le X525
     * android.os.Build.ro.serialno : 7FC38C80C565B260
     * android.os.SystemProperties.android_id : b8ce04f7243fb5a8
     * android.telephony.TelephonyManager.getDeviceId : 867087546618629
     * android.telephony.TelephonyManager.getLine1Number : 15662636406
     * android.telephony.TelephonyManager.getSimCountryIso : cn
     * android.telephony.TelephonyManager.getSimOperator : 46000
     * android.telephony.TelephonyManager.getSimOperatorName : 中国移动
     * android.telephony.TelephonyManager.getSimSerialNumber : 89860062087173558343
     * android.telephony.TelephonyManager.getSimState : 5
     * android.telephony.TelephonyManager.getSubscriberId : 460004192524364
     * android.webview.WebSettings.setUserAgentString : android Mozilla/5.0 (Linux; Android 6.0; Le X525 Build/MDA89D; wv) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 Mobile Safari/537.36
     */

    private long id;
    @SerializedName("android.bluetooth.BluetoothAdapter.MacAddress")
    private String macAddress;
    @SerializedName("android.bluetooth.BluetoothAdapter.name")
    private String bluetoothName;
    @SerializedName("android.content.res.display.dpi")
    private String displayDpi;
    @SerializedName("android.content.res.language")
    private String language;
    @SerializedName("android.location.Location.getLatitude")
    private String latitude;
    @SerializedName("android.location.Location.getLongitude")
    private String longitude;
    @SerializedName("android.net.NetworkInfo.getType")
    private String networkType;
    @SerializedName("android.net.wifi.WifiInfo.NetIpAddr")
    private String wifiIpAddr;
    @SerializedName("android.net.wifi.WifiInfo.getBSSID")
    private String wifiBSSID;
    @SerializedName("android.net.wifi.WifiInfo.getMacAddress")
    private String wifiMacAddress;
    @SerializedName("android.net.wifi.WifiInfo.getSSID")
    private String wifiSSID;
    @SerializedName("android.os.Build.ID")
    private String buildID;
    @SerializedName("android.os.Build.VERSION.RELEASE")
    private String versionRelease;
    @SerializedName("android.os.Build.VERSION.SDK")
    private String versionSDK;
    @SerializedName("android.os.Build.description")
    private String description;
    @SerializedName("android.os.Build.fingerprlong")
    private String fingerprlong;
    @SerializedName("android.os.Build.ro.product.manufacturer")
    private String manufacturer;
    @SerializedName("android.os.Build.ro.product.model")
    private String model;
    @SerializedName("android.os.Build.ro.serialno")
    private String serialno;
    @SerializedName("android.os.SystemProperties.android_id")
    private String android_id;
    @SerializedName("android.telephony.TelephonyManager.getDeviceId")
    private String deviceId;
    @SerializedName("android.telephony.TelephonyManager.getLine1Number")
    private String line1Number;
    @SerializedName("android.telephony.TelephonyManager.getSimCountryIso")
    private String simCountryIso;
    @SerializedName("android.telephony.TelephonyManager.getSimOperator")
    private String simOperator;
    @SerializedName("android.telephony.TelephonyManager.getSimOperatorName")
    private String simOperatorName;
    @SerializedName("android.telephony.TelephonyManager.getSimSerialNumber")
    private String simSerialNumber;
    @SerializedName("android.telephony.TelephonyManager.getSimState")
    private String simState;
    @SerializedName("android.telephony.TelephonyManager.getSubscriberId")
    private String subscriberId;
    @SerializedName("android.webview.WebSettings.setUserAgentString")
    private String userAgent;

    private RealmList<AccountIdsBean> account_ids;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getBluetoothName() {
        return bluetoothName;
    }

    public void setBluetoothName(String bluetoothName) {
        this.bluetoothName = bluetoothName;
    }

    public String getDisplayDpi() {
        return displayDpi;
    }

    public void setDisplayDpi(String displayDpi) {
        this.displayDpi = displayDpi;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getNetworkType() {
        return networkType;
    }

    public void setNetworkType(String networkType) {
        this.networkType = networkType;
    }

    public String getWifiIpAddr() {
        return wifiIpAddr;
    }

    public void setWifiIpAddr(String wifiIpAddr) {
        this.wifiIpAddr = wifiIpAddr;
    }

    public String getWifiBSSID() {
        return wifiBSSID;
    }

    public void setWifiBSSID(String wifiBSSID) {
        this.wifiBSSID = wifiBSSID;
    }

    public String getWifiMacAddress() {
        return wifiMacAddress;
    }

    public void setWifiMacAddress(String wifiMacAddress) {
        this.wifiMacAddress = wifiMacAddress;
    }

    public String getWifiSSID() {
        return wifiSSID;
    }

    public void setWifiSSID(String wifiSSID) {
        this.wifiSSID = wifiSSID;
    }

    public String getBuildID() {
        return buildID;
    }

    public void setBuildID(String buildID) {
        this.buildID = buildID;
    }

    public String getVersionRelease() {
        return versionRelease;
    }

    public void setVersionRelease(String versionRelease) {
        this.versionRelease = versionRelease;
    }

    public String getVersionSDK() {
        return versionSDK;
    }

    public void setVersionSDK(String versionSDK) {
        this.versionSDK = versionSDK;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFingerprlong() {
        return fingerprlong;
    }

    public void setFingerprlong(String fingerprlong) {
        this.fingerprlong = fingerprlong;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialno() {
        return serialno;
    }

    public void setSerialno(String serialno) {
        this.serialno = serialno;
    }

    public String getAndroid_id() {
        return android_id;
    }

    public void setAndroid_id(String android_id) {
        this.android_id = android_id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getLine1Number() {
        return line1Number;
    }

    public void setLine1Number(String line1Number) {
        this.line1Number = line1Number;
    }

    public String getSimCountryIso() {
        return simCountryIso;
    }

    public void setSimCountryIso(String simCountryIso) {
        this.simCountryIso = simCountryIso;
    }

    public String getSimOperator() {
        return simOperator;
    }

    public void setSimOperator(String simOperator) {
        this.simOperator = simOperator;
    }

    public String getSimOperatorName() {
        return simOperatorName;
    }

    public void setSimOperatorName(String simOperatorName) {
        this.simOperatorName = simOperatorName;
    }

    public String getSimSerialNumber() {
        return simSerialNumber;
    }

    public void setSimSerialNumber(String simSerialNumber) {
        this.simSerialNumber = simSerialNumber;
    }

    public String getSimState() {
        return simState;
    }

    public void setSimState(String simState) {
        this.simState = simState;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public RealmList<AccountIdsBean> getAccount_ids() {
        return account_ids;
    }

    public void setAccount_ids(RealmList<AccountIdsBean> account_ids) {
        this.account_ids = account_ids;
    }
}
