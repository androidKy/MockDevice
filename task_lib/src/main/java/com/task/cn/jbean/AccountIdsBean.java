package com.task.cn.jbean;

import java.io.Serializable;

import io.realm.RealmObject;

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
public class AccountIdsBean extends RealmObject implements Serializable {
    /**
     * account_id : 1001
     */

    private long account_id;

    public long getAccount_id() {
        return account_id;
    }

    public void setAccount_id(long account_id) {
        this.account_id = account_id;
    }
}
