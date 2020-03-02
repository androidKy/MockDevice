package com.quin.backup.listener

import com.quin.backup.bean.Result

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
 interface DataListener {
    fun onFinished(result: Result)
}