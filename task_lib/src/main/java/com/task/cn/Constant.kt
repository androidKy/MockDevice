package com.task.cn

/**
 * Description:
 * Created by Quinin on 2020-03-03.
 **/
class URL {
    companion object {
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

class ProxyConstant {
    companion object {
        /**
         * 获取IP
         */
        const val IP_URL = "http://192.168.38.1:8096/open?api=acheqcie&close_time=300&area="  //440000
        const val CITY_CODE_URL = "http://ip.25ios.com:8089/6796324d5300e5978673d71c50780067.php"
        const val PING_URL = "http://pv.sohu.com/cityjson?ie=utf-8"    //https://2020.ip138.com/

        const val DATA_TYPE = "multipart/form-data"
        const val POST_PARAM_METHOD = "method"
        const val POST_PARAM_IMEI = "imei"
        const val POST_PARAM_PLATFORMID = "platformId"
        const val POST_PARAM_AREA = "area"
        const val POST_PARAM_PORT = "port"

        const val SP_CITY_LIST = "sp_city_data"
        //data key
        const val KEY_CITY_DATA = "key_cities"              //城市列表
        const val KEY_CITY_GET_DATE = "key_city_get_date"   //获取城市列表的时间
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