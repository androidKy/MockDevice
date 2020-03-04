package com.task.cn.jbean;

import java.util.List;

import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class AccountInfoBean extends RealmObject {
    /**
     * id : 1001
     * account : 1784389748
     * password : abc123456
     * platform : wechat
     * register_date : 2020-3-3 17:53
     * login_info : [{"login_date":"2020-3-2 17:53","device_id":1003,"login_ip_id":1003},{"login_date":"2020-3-3 17:53","device_id":1004,"login_ip_id":1004}]
     * last_backup_id : 1002
     */
    private long id;
    private String account;
    private String password;
    private String platform;
    private String register_date;
    private long last_backup_id;
    private List<LoginInfoBean> login_info;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getRegister_date() {
        return register_date;
    }

    public void setRegister_date(String register_date) {
        this.register_date = register_date;
    }

    public long getLast_backup_id() {
        return last_backup_id;
    }

    public void setLast_backup_id(long last_backup_id) {
        this.last_backup_id = last_backup_id;
    }

    public List<LoginInfoBean> getLogin_info() {
        return login_info;
    }

    public void setLogin_info(List<LoginInfoBean> login_info) {
        this.login_info = login_info;
    }
}
