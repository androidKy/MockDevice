package com.quin.backup.bean

/**
 * Description:
 * Created by Quinin on 2020-01-17.
 **/
data class FileInfoBean(
    var files: List<FileInfo>,
    val pkg_name: String
)

data class FileInfo(
    val name: String,
    val create_time: String,
    val group_id: String,
    val user_id: String,
    val wr_pm: String
)
