package com.mock.device

import android.app.Application
import com.task.cn.util.AppUtils

/**
 * Description:
 * Created by Quinin on 2020-03-05.
 **/
class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUtils.init(this)
    }
}