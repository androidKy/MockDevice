package com.quin.backup.manager

import android.content.Context
import android.util.Log
import com.quin.TAG
import com.quin.backup.bean.Result
import com.quin.backup.listener.DataListener
import com.quin.backup.permission.PermissionManager
import com.quin.backup.util.CommandUtil
import com.quin.backup.util.FileUtil
import com.quin.backup.util.ThreadUtils
import java.io.File

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class BackupManager private constructor() {

    private var mDataListener: DataListener? = null
    private var mBackupTask: ThreadUtils.Task<Result>? = null
    private var mPkgName: String? = null
    private var mSaveDataDir: String? = null

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            BackupManager()
        }
    }

    fun startBackup(context: Context, pkgName: String, saveDataDir: String, dataListener: DataListener) {
        mDataListener = dataListener
        mPkgName = pkgName
        mSaveDataDir = saveDataDir

        mBackupTask = BackupTask()

        ThreadUtils.executeByCpu(mBackupTask)
    }

    fun cancelBackup() {
        mBackupTask?.apply {
            ThreadUtils.cancel(this)
        }
    }

    inner class BackupTask : ThreadUtils.Task<Result>() {

        override fun doInBackground(): Result {
            val result = Result(Result.FAILED_CODE, "backup failed", null)
            val saveFile = File(mSaveDataDir!!)
            if (!saveFile.exists()) {
                saveFile.mkdir()
            }
            Log.d(TAG, "开始复制${mPkgName}数据")

            val backupShell = "cp -ar /data/data/$mPkgName/* $mSaveDataDir"
            CommandUtil.sendCommand(backupShell, object : CommandUtil.OnResponListener {
                override fun onSuccess(responList: MutableList<String>) {
                    responList.forEach {
                        Log.d(TAG, "copy文件结果:$it")
                    }

                    result.code = Result.SUCCEED_CODE
                    result.msg = "backup succeed"
                    result.file = saveFile

                    /*val fileSize = FileUtil.countFileSize(File(mSaveDataDir))
                    if (fileSize > 0) {
                        result.code = Result.SUCCEED_CODE
                        result.msg = "backup succeed"
                        result.file = File(mSaveDataDir)
                    }*/
                }

                override fun onFailed(msg: String?) {
                    result.code = Result.FAILED_CODE
                    result.msg = "backup failed: $msg"
                }
            })

            return result
        }

        override fun onSuccess(result: Result) {
            Log.d(TAG, "backup thread succeed: $result")
            if (result.code == Result.SUCCEED_CODE) {
                //backupResult(result)
                Log.d(TAG,"复制数据完成")
                startZip(mPkgName!!, result)
            } else {
                backupResult(result)
            }
        }

        override fun onCancel() {
            backupResult(Result(Result.CANCEL_CODE, "backup already cancel", null))
        }

        override fun onFail(t: Throwable?) {
            backupResult(Result(Result.FAILED_CODE, "backup exception:${t?.message}", null))
        }
    }

    private fun backupResult(result: Result) {
        if (result.code == Result.SUCCEED_CODE) {
            deleteBackupData(result)
        }

        mDataListener?.onFinished(result)
    }


    /**
     * 备份成功后删除数据
     */
    private fun deleteBackupData(result: Result) {
        result.file?.apply {
            FileUtil.deleteFile(path)
        }
    }


    private fun startZip(pkgName: String, result: Result) {
        //备份成功，开始压缩
        ZipDataManager.instance.startZip(pkgName, result.file!!, object : DataListener {
            override fun onFinished(result: Result) {
                backupResult(result)
            }
        })
    }

}