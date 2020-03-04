package com.task.cn.jbean;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class TaskBean extends RealmObject {

    /**
     * task_id : 1000
     * account_info : {"id":1001,"account":"1784389748","password":"abc123456","platform":"wechat","register_date":"2020-3-3 17:53","login_info":[{"login_date":"2020-3-2 17:53","device_id":1003,"login_ip_id":1003},{"login_date":"2020-3-3 17:53","device_id":1004,"login_ip_id":1004}],"last_backup_id":1002}
     * backup_info : {"id":1002,"account_id":10001,"device_id":1004,"pkg_name":"com.tencent.mm","is_backuped":false,"backup_data_url":"https://www.baidu.com","last_backup_date":"2020-3-3 17:53"}
     * ip_info : {"id":1003,"ip":"192.168.2.111","city":"广州","city_code":55500}
     * device_info : {"id":1004,"account_ids":[{"account_id":1001},{"account_id":1002}],"android.bluetooth.BluetoothAdapter.MacAddress":"00:12:47:29:09:1E","android.bluetooth.BluetoothAdapter.name":"LeMobile","android.content.res.display.dpi":"","android.content.res.language":"","android.location.Location.getLatitude":"0.0","android.location.Location.getLongitude":"0.0","android.net.NetworkInfo.getType":"3","android.net.wifi.WifiInfo.NetIpAddr":"","android.net.wifi.WifiInfo.getBSSID":"00:18:32:76:BE:E5","android.net.wifi.WifiInfo.getMacAddress":"00:1E:E1:51:41:16","android.net.wifi.WifiInfo.getSSID":"HUAWEI_T4DDGD3","android.os.Build.ID":"MDA89D","android.os.Build.VERSION.RELEASE":"6.0","android.os.Build.VERSION.SDK":"23","android.os.Build.description":"LeMobile-user 6.0 MDA89D ac3ef9367eb5 release-keys","android.os.Build.fingerprlong":"LeMobile/LeMobile/LeMobile:6.0/MDA89D/ac3ef9367eb5:user/release-keys","android.os.Build.ro.product.manufacturer":"LeMobile","android.os.Build.ro.product.model":"Le X525","android.os.Build.ro.serialno":"7FC38C80C565B260","android.os.SystemProperties.android_id":"b8ce04f7243fb5a8","android.telephony.TelephonyManager.getDeviceId":"867087546618629","android.telephony.TelephonyManager.getLine1Number":"15662636406","android.telephony.TelephonyManager.getSimCountryIso":"cn","android.telephony.TelephonyManager.getSimOperator":"46000","android.telephony.TelephonyManager.getSimOperatorName":"中国移动","android.telephony.TelephonyManager.getSimSerialNumber":"89860062087173558343","android.telephony.TelephonyManager.getSimState":"5","android.telephony.TelephonyManager.getSubscriberId":"460004192524364","android.webview.WebSettings.setUserAgentString":"android Mozilla/5.0 (Linux; Android 6.0; Le X525 Build/MDA89D; wv) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.92 Mobile Safari/537.36"}
     */
    @PrimaryKey
    private long id;
    @Required
    private long task_id;

    @Required
    private AccountInfoBean account_info;
    @Required
    private BackupInfoBean backup_info;
    @Required
    private IpInfoBean ip_info;
    @Required
    private DeviceInfoBean device_info;

    /**
     * 0表示任务未开始
     * 1：任务正在进行
     * 2：任务完成
     * -1：任务异常
     */
    @Required
    private int task_status = 0;

    public long getTask_id() {
        return task_id;
    }

    public void setTask_id(long task_id) {
        this.task_id = task_id;
    }

    public AccountInfoBean getAccount_info() {
        return account_info;
    }

    public void setAccount_info(AccountInfoBean account_info) {
        this.account_info = account_info;
    }

    public BackupInfoBean getBackup_info() {
        return backup_info;
    }

    public void setBackup_info(BackupInfoBean backup_info) {
        this.backup_info = backup_info;
    }

    public IpInfoBean getIp_info() {
        return ip_info;
    }

    public void setIp_info(IpInfoBean ip_info) {
        this.ip_info = ip_info;
    }

    public DeviceInfoBean getDevice_info() {
        return device_info;
    }

    public void setDevice_info(DeviceInfoBean device_info) {
        this.device_info = device_info;
    }

    public int getTask_status() {
        return task_status;
    }

    public void setTask_status(int task_status) {
        this.task_status = task_status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
