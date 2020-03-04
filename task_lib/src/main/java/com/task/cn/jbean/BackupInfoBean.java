package com.task.cn.jbean;

import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class BackupInfoBean extends RealmObject {
    /**
     * id : 1002
     * account_id : 10001
     * device_id : 1004
     * pkg_name : com.tencent.mm
     * is_backuped : false
     * backup_data_url : https://www.baidu.com
     * last_backup_date : 2020-3-3 17:53
     */

    private long id;
    private long account_id;
    private long device_id;
    private String pkg_name;
    private boolean is_backuped;
    private String backup_data_url;
    private String last_backup_date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccount_id() {
        return account_id;
    }

    public void setAccount_id(long account_id) {
        this.account_id = account_id;
    }

    public long getDevice_id() {
        return device_id;
    }

    public void setDevice_id(long device_id) {
        this.device_id = device_id;
    }

    public String getPkg_name() {
        return pkg_name;
    }

    public void setPkg_name(String pkg_name) {
        this.pkg_name = pkg_name;
    }

    public boolean isIs_backuped() {
        return is_backuped;
    }

    public void setIs_backuped(boolean is_backuped) {
        this.is_backuped = is_backuped;
    }

    public String getBackup_data_url() {
        return backup_data_url;
    }

    public void setBackup_data_url(String backup_data_url) {
        this.backup_data_url = backup_data_url;
    }

    public String getLast_backup_date() {
        return last_backup_date;
    }

    public void setLast_backup_date(String last_backup_date) {
        this.last_backup_date = last_backup_date;
    }
}
