package com.quin.backup.controller

import android.content.Context
import com.quin.backup.manager.BackupManager
import com.quin.backup.manager.RestoreManager
import com.quin.backup.bean.Result
import com.quin.backup.dir.DataSaveDir
import com.quin.backup.listener.DataListener
import com.quin.backup.permission.PermissionManager
import com.quin.backup.util.ContextUtil
import com.quin.backup.util.ToastUtil

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class DataController private constructor() {

    //private var mDataSaveDir: String? = null

    private var mDataListener: DataListener? = null

    @Volatile
    private var mIsRunning: Boolean = false

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            DataController()
        }
    }

    fun setDataListener(dataListener: DataListener): DataController {
        mDataListener = dataListener
        return this
    }


    /* fun setDataSaveDir(dataSaveDir: String): DataController {
         mDataSaveDir = dataSaveDir
         return this
     }*/

    fun backupData(context: Context, pkgName: String) {
        if (mIsRunning) {
            ToastUtil.showToast(context, "正在备份中...")
            return
        }
        val permissionManager = PermissionManager()

        if (!permissionManager.checkPermission(context)) {
            ToastUtil.showToast(context, "请授予存储权限")
            return
        }

        /*if (!permissionManager.isPhoneRoot()) {
            ToastUtil.showToast(context, "手机未获取root权限")
            return
        }*/

        if (!permissionManager.isAppRoot()) {
            ToastUtil.showToast(context, "应用未获取root权限")
            return
        }

        if (mDataListener == null) {
            ToastUtil.showToast(context, "setDataListener is not called.")
            return
        }

        ContextUtil.setContext(context)

        ToastUtil.showToast(context, "开始备份...")

        mIsRunning = true

        val dataSaveDir = DataSaveDir.getDefaultDir()
        BackupManager.instance.startBackup(context, pkgName, dataSaveDir, object : DataListener {
            override fun onFinished(result: Result) {
                mIsRunning = false
                mDataListener?.onFinished(result)
            }
        })
    }

    fun cancalBackup(context: Context) {
        if (!mIsRunning) {
            ToastUtil.showToast(context, "没有备份任务正在执行")
            return
        }
        ContextUtil.setContext(context)
        BackupManager.instance.cancelBackup()
    }


    fun restoreData(context: Context, pkgName: String) {
        if (mIsRunning) {
            ToastUtil.showToast(context, "正在还原中...")
            return
        }

        val permissionManager = PermissionManager()

        if (!permissionManager.checkPermission(context)) {
            ToastUtil.showToast(context, "请授予存储权限")
            return
        }

        /*if (!permissionManager.isPhoneRoot()) {
            ToastUtil.showToast(context, "手机未获取root权限")
            return
        }*/

        if (!permissionManager.isAppRoot()) {
            ToastUtil.showToast(context, "应用未获取root权限")
            return
        }

        ToastUtil.showToast(context, "正在还原...")
        ContextUtil.setContext(context)

        mIsRunning = true
        val dataSaveDir = DataSaveDir.getDefaultDir()
        mDataListener?.apply {
            RestoreManager.instantce.startRestore(pkgName, dataSaveDir, object : DataListener {
                override fun onFinished(result: Result) {
                    mIsRunning = false
                    mDataListener?.onFinished(result)
                }
            })
          /*  PermissionManager().closeSelinusFile(object : DataListener {
                override fun onFinished(result: Result) {

                }
            })*/
        }
    }

    fun cancelRestore(context: Context) {
        if (!mIsRunning) {
            ToastUtil.showToast(context, "没有备份任务正在执行")
            return
        }
        ContextUtil.setContext(context)
        RestoreManager.instantce.cancelTask()
    }

    fun destroy() {
        ContextUtil.clear()
    }
}