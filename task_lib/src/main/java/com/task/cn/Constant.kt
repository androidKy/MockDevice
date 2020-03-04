package com.task.cn

/**
 * Description:
 * Created by Quinin on 2020-03-03.
 **/
class URL {
    companion object {
        /**
         * 获取IP
         */
        const val GET_IP_URL = ""
        /**
         * 获取设备信息
         */
        const val GET_DEVICE_INFO_URL = ""
        /**
         * 上传任务信息
         * 1、账号信息
         * 2、设备信息
         * 3、IP信息
         */
        const val UPLOAD_TASK_INFO_URL = ""
        /**
         * 获取任务信息
         */
        const val GET_TASK_INFO_URL = ""
    }
}

data class Result<R>(var code: StatusCode, var r: R, var msg: String)


enum class StatusCode(val code: Int) {
    SUCCEED(200),
    FAILED(400)
}

enum class StatusMsg(val msg: String) {
    DEFAULT("default failed"),
    SUCCEED("succeed")
}

enum class StatusTask(val taskStatus: Int) {
    TASK_UNSTART(0),
    TASK_RUNNING(1),
    TASK_FINISHED(2),
    TASK_EXCEPTION(-1)
}