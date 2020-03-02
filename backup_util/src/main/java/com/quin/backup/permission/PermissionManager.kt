package com.quin.backup.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.quin.TAG
import com.quin.backup.bean.FileInfo
import com.quin.backup.bean.FileInfoBean
import com.quin.backup.bean.Result
import com.quin.backup.listener.DataListener
import com.quin.backup.listener.FilePerListener
import com.quin.backup.util.CommandUtil
import com.quin.backup.util.ContextUtil
import com.quin.backup.util.ThreadUtils
import java.io.DataOutputStream
import java.io.File

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class PermissionManager {
    private var mDataListener: DataListener? = null
    private var mFilePListener: FilePerListener? = null
    private var mPkgName: String? = null
    private var mRcFilePTask: RecoverFilePermissionTask? = null
    private var mFileInfoBean: FileInfoBean? = null

    private val permissionsRequest = mutableListOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)

    /**
     *  检测app是否有读写权限
     */
    fun checkPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val iterator = permissionsRequest.iterator()
            while (iterator.hasNext()) {
                val permission = iterator.next()
                if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                    iterator.remove()
                }
            }
            permissionsRequest.size == 0
        } else {
            true
        }
    }

    fun isPhoneRoot(): Boolean {
        //File("/system/bin/su").exists()
        try {
            if (File("/system/xbin/su").exists())
                return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun isAppRoot(): Boolean {
        var process: Process? = null
        var outputStream: DataOutputStream? = null

        try {
            process = Runtime.getRuntime().exec("su")
            outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            return process.waitFor() == 0
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.apply {
                close()
            }

            process?.apply {
                destroy()
            }
        }

        return false
    }


    /**
     * 恢复APP打开的请求权限和文件的权限
     */
    fun recoverPermissions(pkgName: String?, dataListener: DataListener?, fileInfoBean: FileInfoBean?) {
        mPkgName = pkgName
        mDataListener = dataListener
        mFileInfoBean = fileInfoBean

        recoverAppPermissions()
    }

    private fun recoverAppPermissions() {
        //todo 恢复app的权限

        recoverFilePermissions()
    }

    private fun recoverFilePermissions() {
        mRcFilePTask = RecoverFilePermissionTask()
        ThreadUtils.executeByCached(mRcFilePTask)
    }

    /**
     * 保存文件的权限
     */
    fun getFilePermissions(pkgName: String, filePerListener: FilePerListener) {
        mFilePListener = filePerListener
        mPkgName = pkgName
        ThreadUtils.executeByCached(SaveFilePermissionTask())
    }

    private inner class SaveFilePermissionTask : ThreadUtils.Task<FileInfoBean>() {

        override fun doInBackground(): FileInfoBean {
            //val result = Result(Result.FAILED_CODE, "save file permission failed.", null)
            Log.d(TAG, "保存文件相应的权限")
            val fileList = ArrayList<FileInfo>()
            val fileInfoBean = FileInfoBean(fileList, mPkgName!!)

            val lsShell = "ls -l /data/data/$mPkgName"

            CommandUtil.sendCommand(lsShell, object : CommandUtil.OnResponListener {
                override fun onSuccess(responList: MutableList<String>) {
                    Log.d(TAG, "getFilePermissions: responList size = ${responList.size}")
                    try {
                        for (line in responList) {
                            //drwxrwx--x u0_a140 u0_a140    2020-01-16 21:15 app_textures
                            val fileInfos = line.split("\\s+".toRegex())
                            if (fileInfos.isEmpty())
                                return

                            val rwPer = fileInfos[0]
                            var appUserId = ""
                            val fileName = fileInfos[fileInfos.size - 1]

                            fileInfos.forEach breaking@{
                                if (it.startsWith("u0_")) {
                                    appUserId = it
                                    return@breaking
                                }
                            }
                            fileList.add(FileInfo(fileName, "", "",
                                    appUserId, rwPer))
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "SaveFilePermissionTask:${e.message!!}")
                    }

                    fileInfoBean.files = fileList
                    Log.d(TAG, "result: $fileInfoBean")
                }

                override fun onFailed(msg: String?) {
                    Log.d(TAG, "save permission failed: $msg")
                }
            })

            return fileInfoBean
        }

        override fun onSuccess(fileInfoBean: FileInfoBean) {
            mFilePListener?.onFilePermission(fileInfoBean)
        }

        override fun onCancel() {
        }

        override fun onFail(t: Throwable?) {
        }
    }


    private inner class RecoverFilePermissionTask : ThreadUtils.Task<Result>() {

        override fun doInBackground(): Result {
            Log.d(TAG, "恢复文件相应的权限")
            val result = Result(Result.FAILED_CODE, "recover file permission failed.", null)

            var appUserId = ""
            mFileInfoBean?.apply {
                if (pkg_name == mPkgName && files.isNotEmpty()) {
                    appUserId = files[1].user_id
                }
            }

            val userPermission = "chown -R $appUserId:$appUserId /data/data/$mPkgName;"
            val rwPermission = "chmod -R 771 /data/data/$mPkgName/;"
            Log.d(TAG, "userPermission: $userPermission\nrwPermission: $rwPermission")

            CommandUtil.sendCommand(rwPermission + userPermission, object : CommandUtil.OnResponListener {
                override fun onSuccess(responList: MutableList<String>?) {
                    responList?.apply {
                        forEach {
                            Log.d(TAG, "recover permission: $it")
                        }
                    }
                    result.code = Result.SUCCEED_CODE
                    result.msg = "succeed"
                }

                override fun onFailed(msg: String?) {
                    Log.d(TAG, "recover permission failed:$msg")
                }
            })

            return result
        }

        override fun onSuccess(result: Result) {
            mDataListener?.onFinished(result)
        }

        override fun onCancel() {

        }

        override fun onFail(t: Throwable?) {
            Log.d(TAG, "restore permission failed: ${t?.message}")
        }
    }

    fun closeSelinusFile(dataListener: DataListener?) {
        ThreadUtils.executeByCached(object : ThreadUtils.Task<Boolean>() {
            override fun doInBackground(): Boolean {
                val command = "setenforce 0"
                CommandUtil.sendCommand(command)

                return true
            }

            override fun onSuccess(result: Boolean?) {
                dataListener?.onFinished(Result(Result.SUCCEED_CODE, "succeed", null))
            }

            override fun onCancel() {
            }

            override fun onFail(t: Throwable?) {
            }

        })
    }
}