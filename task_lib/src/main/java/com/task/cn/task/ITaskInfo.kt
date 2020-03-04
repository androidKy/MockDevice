package com.task.cn.task

import com.task.cn.jbean.TaskBean


/**
 * Description:
 * Created by Quinin on 2020-03-03.
 **/
interface ITaskInfo {
    fun getTaskInfo()
    fun getAccountInfo()
    fun getIpInfo()
    fun getDeviceInfo()

    fun getLocationByIP(ip:String)

    fun changeDeviceInfo(taskBean: TaskBean)
}