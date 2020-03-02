package com.quin.backup.util

import java.io.File

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class FileUtil {
    companion object {
        fun countFileSize(file: File): Long {
            var fileSize: Long = 0

            if (!file.exists())
                return fileSize

            if (file.isDirectory) {
                val files = file.listFiles()
                if (files == null || files.isEmpty())
                    return fileSize

                files.forEach {
                    fileSize += if (it.isFile)
                        it.length()
                    else countFileSize(file)
                }

            } else fileSize = file.length()

            return fileSize
        }

        fun deleteFile(path: String) {
            val file = File(path)
            if (file.exists()) {
                ThreadUtils.executeByCached(object : ThreadUtils.Task<Boolean>() {
                    override fun doInBackground(): Boolean {
                        val shell = "cd $path;" +
                                "rm -fr *"

                        CommandUtil.sendCommand(shell)

                        return true
                    }

                    override fun onSuccess(result: Boolean?) {
                        file.delete()
                    }

                    override fun onCancel() {
                    }

                    override fun onFail(t: Throwable?) {
                    }
                })
            }
        }
    }
}