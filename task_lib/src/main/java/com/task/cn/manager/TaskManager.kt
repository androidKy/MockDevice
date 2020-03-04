package com.task.cn.manager

import com.task.cn.StatusTask
import com.task.cn.task.ITaskControllerView
import com.task.cn.task.TaskControllerImpl
import com.utils.common.ToastUtils

/**
 * Description:任务管理
 * Created by Quinin on 2020-03-03.
 **/
class TaskManager(private val taskBuilder: TaskBuilder) {

    fun startTask() {
        if(taskBuilder.mITaskControllerVIew == null)
        {
            ToastUtils.showToast("ITaskControllerView must not be null")
            return
        }
        taskBuilder.mITaskControllerVIew?.run {
            TaskControllerImpl(this).startTask(taskBuilder)
        }
    }
}

//组装组件
interface Builder<out T> {
    fun build(): T
}

class TaskBuilder : Builder<TaskManager> {

    var mTaskInfoSwitch: Boolean = true

    var mIpSwitch: Boolean = false
    var mAccountSwitch: Boolean = false
    var mDeviceSwitch: Boolean = false

    var mLastTaskStatus: StatusTask = StatusTask.TASK_FINISHED

    var mITaskControllerVIew: ITaskControllerView? = null

    fun setTaskControllerView(taskControllerView: ITaskControllerView): TaskBuilder {
        this.mITaskControllerVIew = taskControllerView

        return this
    }

    /**
     * 是否更新ip
     */
    fun setIpSwitch(ipSwitch: Boolean): TaskBuilder {
        this.mIpSwitch = ipSwitch

        return this
    }

    /**
     * 是否更新账号
     */
    fun setAccountSwitch(accountSwitch: Boolean): TaskBuilder {
        this.mAccountSwitch = accountSwitch
        return this
    }

    /**
     * 是否更改设备信息
     */
    fun setDeviceSwitch(deviceSwitch: Boolean): TaskBuilder {
        this.mDeviceSwitch = deviceSwitch
        return this
    }

    /**
     * 是否从服务器获取任务信息
     */
    fun setTaskInfoSwitch(taskInfoSwitch: Boolean): TaskBuilder {
        this.mTaskInfoSwitch = taskInfoSwitch
        return this
    }

    /**
     * 设置上次任务的执行状态
     */
    fun setLastTaskStatus(lastTaskStatus: StatusTask): TaskBuilder {
        this.mLastTaskStatus = lastTaskStatus
        return this
    }

    override fun build(): TaskManager {
        return TaskManager(this)
    }
}

