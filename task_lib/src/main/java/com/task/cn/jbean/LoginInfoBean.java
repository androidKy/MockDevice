package com.task.cn.jbean;

import java.io.Serializable;

import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class LoginInfoBean extends RealmObject implements Serializable {
    /**
     * login_date : 2020-3-2 17:53
     * device_id : 1003
     * login_ip_id : 1003
     */

    private String login_date;
    private long device_id;
    private long login_ip_id;

    public String getLogin_date() {
        return login_date;
    }

    public void setLogin_date(String login_date) {
        this.login_date = login_date;
    }

    public long getDevice_id() {
        return device_id;
    }

    public void setDevice_id(long device_id) {
        this.device_id = device_id;
    }

    public long getLogin_ip_id() {
        return login_ip_id;
    }

    public void setLogin_ip_id(long login_ip_id) {
        this.login_ip_id = login_ip_id;
    }
}
