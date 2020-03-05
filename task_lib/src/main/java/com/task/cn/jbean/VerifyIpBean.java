package com.task.cn.jbean;

/**
 * Description:
 * Created by Quinin on 2020-03-05.
 **/
public class VerifyIpBean {

    /**
     * cip : 58.62.203.108
     * cid : 440111
     * cname : 广东省广州市白云区
     */

    private String cip;
    private String cid;
    private String cname;

    public String getCip() {
        return cip;
    }

    public void setCip(String cip) {
        this.cip = cip;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    @Override
    public String toString() {
        return "VerifyIpBean{" +
                "cip='" + cip + '\'' +
                ", cid='" + cid + '\'' +
                ", cname='" + cname + '\'' +
                '}';
    }
}
