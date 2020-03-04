package com.task.cn.util

import android.content.Context
import com.utils.common.Utils
import io.realm.Realm

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
class AppUtils {
    companion object{
        fun init(context:Context){
            Utils.init(context)
            Realm.init(context)
        }
    }
}