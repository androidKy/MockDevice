package com.quin.backup.util

import android.content.Context
import android.content.SharedPreferences
import java.lang.ref.WeakReference

/**
 * Description:
 * Created by Quinin on 2020-01-18.
 **/
class SPUtil private constructor() {
    private var mWRcontext: WeakReference<Context>? = null
    private var mSPname: String? = null

    companion object {

        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            SPUtil()
        }
        /* private var instance: SPUtil<Any, Any>? = null

         fun getInstance(): SPUtil<Any, Any> {
             if (instance == null) {
                 synchronized(SPUtil::class.java) {
                     if (instance == null)
                         instance = SPUtil()
                 }
             }
             return instance!!
         }*/
    }

    fun build(context: Context, spName: String): SPUtil {
        mWRcontext = WeakReference(context.applicationContext)
        mSPname = spName
        return this
    }


    private fun getEditor(): SharedPreferences.Editor? {
        var editor: SharedPreferences.Editor? = null
        mWRcontext?.get()?.run {
            editor = this.getSharedPreferences(mSPname, Context.MODE_PRIVATE)?.edit()
        }

        return editor
    }

    fun put(key: String, value: Any, isCommit: Boolean) {
        if (mWRcontext?.get() == null || mSPname.isNullOrEmpty())
            return
        getEditor()?.apply {
            when (value) {
                is String -> {
                    putString(key, value)
                }
                is Int -> {
                    putInt(key, value)
                }
                is Boolean -> {
                    putBoolean(key, value)
                }
                is Float -> {
                    putFloat(key, value)
                }
                is Long -> {
                    putLong(key, value)
                }
            }

            if (isCommit)
                commit()
            else apply()
        }
    }

    fun put(key: String, value: Any) {
        put(key, value, false)
    }

    private fun getSP(): SharedPreferences {
        val context = mWRcontext?.get()!!
        return context.getSharedPreferences(mSPname, Context.MODE_PRIVATE)
    }

    fun getString(key: String): String {

        return getSP().getString(key, "")!!
    }

    fun getInt(key: String): Int {
        return getSP().getInt(key, 0)
    }

    fun getLong(key: String): Long {
        return getSP().getLong(key, 0L)
    }

    fun getFloat(key: String): Float {
        return getSP().getFloat(key, 0f)
    }

}