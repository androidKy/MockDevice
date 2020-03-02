package com.quin.backup.manager

import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.quin.TAG
import com.quin.backup.bean.Result
import com.quin.backup.dir.DataSaveDir
import com.quin.backup.listener.DataListener
import com.quin.backup.util.ThreadUtils
import com.quin.backup.zip.ZipUtils
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

/**
 * Description:压缩数据
 * Created by Quinin on 2020-01-15.
 **/
class ZipDataManager private constructor() {

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            ZipDataManager()
        }
    }

    private var mDataListener: DataListener? = null
    private var mFileDir: File? = null
    private var mZipFileUtil: ZipFile? = null


    private val mHandler: Handler = Handler(Looper.getMainLooper()) {
        if (it.what == 2000) {
            mZipFileUtil?.apply {
                val progressMonitor = progressMonitor
                when (progressMonitor.result) {
                    ProgressMonitor.Result.SUCCESS -> {
                        mDataListener?.onFinished(Result(Result.SUCCEED_CODE, "zip succeed", mFileDir))
                    }
                    ProgressMonitor.Result.ERROR -> {
                        mDataListener?.onFinished(Result(Result.FAILED_CODE, "zip failed", mFileDir))
                    }
                    ProgressMonitor.Result.CANCELLED -> {
                        mDataListener?.onFinished(Result(Result.CANCEL_CODE, "zip canceled", mFileDir))
                    }

                    else -> {
                        Log.d(TAG, "正在压缩: ${progressMonitor.percentDone}")
                        //mHandler.sendEmptyMessageDelayed(2000, 500)
                        getZipState()
                    }
                }
            }
        }
        false
    }

    fun startZip(pkgName: String, fileDir: File, dataListener: DataListener) {
        Log.d(TAG, "开始压缩...")
        mDataListener = dataListener
        mFileDir = fileDir

        try {
            val zipFileDir = DataSaveDir.getZipFileDir()
            val zipDirFile = File(zipFileDir)
            //存放压缩文件的目录是否存在
            if (!zipDirFile.exists()) {
                //zipDirFile.delete()
                zipDirFile.mkdir()
            }
            //压缩文件是否存在
            val zipFile = DataSaveDir.getZipFile(pkgName)
            if (zipFile.exists()) {
                zipFile.delete()
            }

            Log.d(TAG, "zip file dir: $mFileDir")
            /*  ThreadUtils.executeByCached(object : ThreadUtils.Task<Boolean>() {
                  override fun doInBackground(): Boolean {
                      try {
                          val sourceFile = File(DataSaveDir.getDefaultDir())
                          if (sourceFile.exists())
                              ZipUtils.zip(sourceFile, zipFile.path)
                      } catch (e: Exception) {
                          Log.d(TAG, e.message, e)
                          return false
                      }
                      return true
                  }

                  override fun onSuccess(result: Boolean) {
                      if (result)
                          dataListener.onFinished(Result(Result.SUCCEED_CODE, "succeed", zipFile))
                      else dataListener.onFinished(Result(Result.FAILED_CODE, "zip failed", null))
                  }

                  override fun onCancel() {

                  }

                  override fun onFail(t: Throwable?) {
                  }
              })*/
            mZipFileUtil = ZipFile(zipFile).apply {
                //isRunInThread = true
                addFolder(File("$mFileDir${File.separator}"))
            }
            getZipState()
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }

    }

    private fun getZipState() {
        mHandler.sendEmptyMessageDelayed(2000, 500)
    }

    fun extract(pkgName: String, fileDir: String, dataListener: DataListener) {
        Log.d(TAG, "正在解压...")
        mDataListener = dataListener
        Log.d(TAG, "compress file dir: $fileDir")
        val backupFile = File(fileDir)
        if (backupFile.exists())
            backupFile.delete()
        val sdcardDir = Environment.getExternalStorageDirectory().path
        try {
            //val zipFileDir = DataSaveDir.getZipFileDir()
            val zipFile = DataSaveDir.getZipFile(pkgName)
            if (!zipFile.exists()) {
                mDataListener?.onFinished(Result(Result.FAILED_CODE, "extract failed: zip file not exist", null))
                return
            }

            mZipFileUtil = ZipFile(zipFile)
            mZipFileUtil?.extractAll(sdcardDir)
            getZipState()
            /*ThreadUtils.executeByCached(object: ThreadUtils.Task<Boolean>(){
                override fun doInBackground(): Boolean {
                    return false
                }

                override fun onSuccess(result: Boolean?) {

                }

                override fun onCancel() {

                }

                override fun onFail(t: Throwable?) {

                }
            })*/
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }

    }

}