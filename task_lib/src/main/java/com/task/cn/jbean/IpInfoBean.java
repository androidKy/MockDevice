package com.task.cn.jbean;

import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class IpInfoBean extends RealmObject {
    /**
     * id : 1003
     * ip : 192.168.2.111
     * city : 广州
     * city_code : 55500
     */

    private long id;
    private String ip;
    private String city;
    private long city_code;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getCity_code() {
        return city_code;
    }

    public void setCity_code(long city_code) {
        this.city_code = city_code;
    }
}
