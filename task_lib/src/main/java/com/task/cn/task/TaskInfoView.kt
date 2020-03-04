package com.task.cn.task

import com.task.cn.jbean.AccountInfoBean
import com.task.cn.jbean.DeviceInfoBean
import com.task.cn.jbean.IpInfoBean
import com.task.cn.jbean.TaskBean
import com.task.cn.Result

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
interface TaskInfoView {
    fun onResponTaskInfo(result: Result<TaskBean>)

    fun onResponIpInfo(result: Result<IpInfoBean>)

    fun onResponDeviceInfo(result: Result<DeviceInfoBean>)

    fun onResponAccountInfo(result: Result<AccountInfoBean>)

    fun onResponIPAddress(latitude: String, longitude: String)

    fun onChangeDeviceInfo(result: Result<Boolean>)

}