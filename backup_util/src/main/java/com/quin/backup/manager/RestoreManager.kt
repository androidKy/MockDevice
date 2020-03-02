package com.quin.backup.manager

import android.util.Log
import com.quin.TAG
import com.quin.backup.bean.FileInfoBean
import com.quin.backup.bean.Result
import com.quin.backup.dir.DataSaveDir
import com.quin.backup.listener.DataListener
import com.quin.backup.listener.FilePerListener
import com.quin.backup.permission.PermissionManager
import com.quin.backup.util.CommandUtil
import com.quin.backup.util.ThreadUtils

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class RestoreManager private constructor() {

    private var mDataListener: DataListener? = null
    private var mPkgName: String? = null
    private var mDataSaveDir: String? = null
    private var mRestoreTask: ThreadUtils.Task<Result>? = null
    private var mFileInfoBean: FileInfoBean? = null

    companion object {
        val instantce by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            RestoreManager()
        }
    }

    fun startRestore(pkgName: String, dataSaveDir: String, dataListener: DataListener) {
        mPkgName = pkgName
        mDataSaveDir = dataSaveDir

        val zipFile = DataSaveDir.getZipFile(pkgName)

        if (!zipFile.exists()) {
            dataListener.onFinished(Result(Result.FAILED_CODE, "restore failed: zip file not found.", null))
            return
        }

        mDataListener = dataListener

        //先查找文件的权限
        getFilePermission()
    }

    private fun getFilePermission() {
        PermissionManager().getFilePermissions(mPkgName!!, object : FilePerListener {
            override fun onFilePermission(fileInfoBean: FileInfoBean) {
                mFileInfoBean = fileInfoBean
                compressData()
            }
        })
    }

    private fun compressData() {
        //解压到指定的文件，然后copy数据到包名下，再恢复权限
        ZipDataManager.instance.extract(mPkgName!!, mDataSaveDir!!, object : DataListener {
            override fun onFinished(result: Result) {
                Log.d(TAG, "after unzip: $result")
                if (result.code == Result.SUCCEED_CODE) {
                    copyData2app()
                } else mDataListener?.onFinished(result)
            }
        })
    }

    /**
     * 把数据copy到data/data/pkgName/下
     */
    private fun copyData2app() {
        mRestoreTask = RestoreTask()
        ThreadUtils.executeByCached(mRestoreTask)
    }

    fun cancelTask() {
        mRestoreTask?.apply {
            ThreadUtils.cancel(this)
        }
    }

    private inner class RestoreTask : ThreadUtils.Task<Result>() {

        override fun doInBackground(): Result {
            val result = Result(Result.FAILED_CODE, "restore failed", null)
            val closeAppShell = "am force-stop $mPkgName;"
            val clearShell = "rm -fr /data/data/$mPkgName/*;"
            val restoreShell = "cp -ar $mDataSaveDir/* /data/data/$mPkgName/;"

            CommandUtil.sendCommand(closeAppShell+clearShell + restoreShell, object : CommandUtil.OnResponListener {
                override fun onFailed(msg: String?) {
                    result.code = Result.FAILED_CODE
                    result.msg = "backup failed: $msg"
                }

                override fun onSuccess(responList: MutableList<String>?) {
                    responList?.forEach {
                        Log.d(TAG, "restore文件结果:$it")
                    }
                    result.code = Result.SUCCEED_CODE
                    result.msg = "success"
                }
            })
            return result
        }

        override fun onSuccess(result: Result) {
            if (result.code == Result.SUCCEED_CODE) {
                PermissionManager().recoverPermissions(mPkgName, mDataListener, mFileInfoBean)
            }
        }

        override fun onCancel() {
            backupResult(Result(Result.FAILED_CODE, "restore was canceled.", null))
        }

        override fun onFail(t: Throwable?) {
            backupResult(Result(Result.FAILED_CODE, "restore failed: ${t?.message}", null))
        }
    }

    private fun backupResult(result: Result) {
        mDataListener?.onFinished(result)
    }
}