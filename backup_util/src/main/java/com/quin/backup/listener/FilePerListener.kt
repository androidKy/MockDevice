package com.quin.backup.listener

import com.quin.backup.bean.FileInfoBean

/**
 * Description:
 * Created by Quinin on 2020-01-18.
 **/
interface FilePerListener {
    fun onFilePermission(fileInfoBean: FileInfoBean)
}