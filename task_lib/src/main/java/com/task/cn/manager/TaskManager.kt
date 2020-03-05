package com.task.cn.manager

import com.task.cn.StatusTask
import com.task.cn.task.ITaskControllerView
import com.task.cn.task.TaskControllerImpl
import com.utils.common.ToastUtils

/**
 * Description:任务管理
 * Created by Quinin on 2020-03-03.
 **/
class TaskManager private constructor(private val taskBuilder: TaskBuilder) {

    fun startTask() {
        if (taskBuilder.getTaskControllerView() == null) {
            //ToastUtils.showToast("ITaskControllerView must not be null")
            return
        }
        taskBuilder.getTaskControllerView()?.run {
            TaskControllerImpl(this).startTask(taskBuilder)
        }
    }

    companion object {
        class TaskBuilder : Builder<TaskManager> {
            private var mTaskInfoSwitch: Boolean = true

            private var mIpSwitch: Boolean = false
            private var mAccountSwitch: Boolean = false
            private var mDeviceSwitch: Boolean = false

            private var mLastTaskStatus: StatusTask = StatusTask.TASK_FINISHED

            private var mITaskControllerVIew: ITaskControllerView? = null

            private var mCityName: String = ""

            fun setCityName(cityName: String): TaskBuilder {
                this.mCityName = cityName
                return this
            }

            fun getCityName(): String {
                return mCityName
            }

            /**
             * 设置任务监听
             */
            fun setTaskControllerView(taskControllerView: ITaskControllerView): TaskBuilder {
                this.mITaskControllerVIew = taskControllerView

                return this
            }

            fun getTaskControllerView(): ITaskControllerView? {
                return mITaskControllerVIew
            }

            /**
             * 是否更新ip
             */
            fun setIpSwitch(ipSwitch: Boolean): TaskBuilder {
                this.mIpSwitch = ipSwitch

                return this
            }

            fun getIpSwitch(): Boolean {
                return mIpSwitch
            }

            /**
             * 是否更新账号
             */
            fun setAccountSwitch(accountSwitch: Boolean): TaskBuilder {
                this.mAccountSwitch = accountSwitch
                return this
            }

            fun getAccountSwitch(): Boolean {
                return mAccountSwitch
            }

            /**
             * 是否更改设备信息
             */
            fun setDeviceSwitch(deviceSwitch: Boolean): TaskBuilder {
                this.mDeviceSwitch = deviceSwitch
                return this
            }

            fun getDeviceSwitch(): Boolean {
                return mDeviceSwitch
            }

            /**
             * 是否从服务器获取任务信息
             */
            fun setTaskInfoSwitch(taskInfoSwitch: Boolean): TaskBuilder {
                this.mTaskInfoSwitch = taskInfoSwitch
                return this
            }

            fun getTaskInfoSwitch(): Boolean {
                return mTaskInfoSwitch
            }

            /**
             * 设置上次任务的执行状态
             */
            fun setLastTaskStatus(lastTaskStatus: StatusTask): TaskBuilder {
                this.mLastTaskStatus = lastTaskStatus
                return this
            }

            fun getLastTaskStatus(): StatusTask {
                return mLastTaskStatus
            }

            override fun build(): TaskManager {
                return TaskManager(this)
            }

        }
    }
}

//组装组件
interface Builder<out T> {
    fun build(): T
}


