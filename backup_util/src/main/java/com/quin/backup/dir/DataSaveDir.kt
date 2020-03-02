package com.quin.backup.dir

import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Description:备份数据保存的路径
 * Created by Quinin on 2020-01-15.
 **/
class DataSaveDir private constructor() {

    companion object {
        /**
         * 获取备份数据存储的路径
         */
        fun getSaveDir(defineDir: String?): String {
            if (defineDir.isNullOrEmpty())
                return getDefaultDir()

            return defineDir
        }

        fun getDefaultDir(): String {
            var defaultDir: String = Environment.getExternalStorageDirectory().absolutePath + File.separator + "backupData"

           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)  //Android版本大于9.0不允许访问sdcard
            {
                //在AndroidManifes.xml添加属性android:requestLegacyExternalStorage="true"进行适配
            } else {
                defaultDir = Environment.getExternalStorageDirectory().absolutePath + File.separator + "backupData"
            }*/

            return defaultDir
        }

        /**
         * 获取压缩文件存储的路径
         */
        fun getZipFileDir(): String {
            return Environment.getExternalStorageDirectory().absolutePath +
                    File.separator + "zipFileDir" + File.separator
        }

        fun getZipFile(pkgName: String): File {
            return File(getZipFileDir() + getZipFileName(pkgName))
        }

        private fun getZipFileName(pkgName: String): String {
            return "${pkgName.replace(".", "_")}.zip"
        }
    }


}