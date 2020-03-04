package com.task.cn.task

import android.os.Handler
import android.os.Looper
import com.safframework.log.L
import com.task.cn.Result
import com.task.cn.StatusCode
import com.task.cn.StatusTask
import com.task.cn.database.RealmHelper
import com.task.cn.jbean.AccountInfoBean
import com.task.cn.jbean.DeviceInfoBean
import com.task.cn.jbean.IpInfoBean
import com.task.cn.jbean.TaskBean
import com.task.cn.manager.TaskBuilder
import com.utils.common.ToastUtils

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
class TaskControllerImpl(private val taskControllerView: ITaskControllerView) : ITaskController, TaskInfoView {

    companion object {
        const val MSG_TASK_COUNT: Int = 1000
    }

    @Volatile
    private var mTaskStatus: StatusTask = StatusTask.TASK_UNSTART
    @Volatile
    private var mTaskStartCount: Int = 0    //已开始的任务数量
    @Volatile
    private var mTaskErrorCount: Int = 0  //错误的任务数量

    @Volatile
    private var mTaskBean: TaskBean = TaskBean()
    @Volatile
    private var mRealmHelper: RealmHelper = RealmHelper()

    private var mTaskExecutor: TaskInfoImpl? = null

    private val mHandler: Handler = Handler(Looper.getMainLooper()) {
        if (it.what == MSG_TASK_COUNT) {
            if (mTaskStartCount == 0) { //已开始的任务完成
                if (mTaskErrorCount == 0) {
                    mTaskExecutor?.getLocationByIP(mTaskBean.ip_info.ip)
                } else {
                    taskControllerView.onTaskPrepared(Result(StatusCode.FAILED, false, "ready failed"))
                }
            } else {
                sendTaskMsg(500)
            }
        }

        false
    }

    override fun startTask(taskBuilder: TaskBuilder) {
        /**
         * 上次的任务未执行完成
         */
        if (taskBuilder.mLastTaskStatus == StatusTask.TASK_RUNNING) {
            ToastUtils.showToast("上次的任务未执行完成")
            setTaskStatus(StatusTask.TASK_RUNNING)
            return
        }

        setTaskStatus(StatusTask.TASK_RUNNING)

        mTaskStartCount = 0
        mTaskErrorCount = 0

        mTaskExecutor = TaskInfoImpl(this)

        if (taskBuilder.mTaskInfoSwitch) {
            mTaskStartCount++
            mTaskExecutor?.getTaskInfo()
        }

        if (taskBuilder.mIpSwitch) {
            mTaskStartCount++
            mTaskExecutor?.getIpInfo()
        }

        if (taskBuilder.mAccountSwitch) {
            mTaskStartCount++
            mTaskExecutor?.getAccountInfo()
        }

        if (taskBuilder.mDeviceSwitch) {
            mTaskStartCount++
            mTaskExecutor?.getDeviceInfo()
        }
    }

    override fun onResponTaskInfo(result: Result<TaskBean>) {
        if (result.code == StatusCode.FAILED) {
            dealError(result.msg)
        } else
            mTaskBean = result.r
        sendTaskResult()
    }

    override fun onResponIpInfo(result: Result<IpInfoBean>) {
        if (result.code == StatusCode.FAILED) {
            dealError(result.msg)
        } else
            mTaskBean.ip_info = result.r
        sendTaskResult()
    }

    override fun onResponDeviceInfo(result: Result<DeviceInfoBean>) {
        if (result.code == StatusCode.FAILED) {
            dealError(result.msg)
        } else
            mTaskBean.device_info = result.r
        sendTaskResult()
    }

    override fun onResponAccountInfo(result: Result<AccountInfoBean>) {
        if (result.code == StatusCode.FAILED) {
            dealError(result.msg)
        } else
            mTaskBean.account_info = result.r
        sendTaskResult()
    }

    override fun onResponIPAddress(latitude: String, longitude: String) {
        L.d("根据IP获取的经纬度: latitude=$latitude longitude=$longitude")

        if (latitude.isNullOrEmpty() || longitude.isNullOrEmpty()) {
            dealError("获取经纬度失败")
            taskControllerView.onTaskPrepared(Result(StatusCode.FAILED, false, "获取经纬度失败"))
            return
        }

        mTaskBean.device_info.latitude = latitude
        mTaskBean.device_info.longitude = longitude

        mTaskExecutor?.changeDeviceInfo(mTaskBean)
    }

    override fun onChangeDeviceInfo(result: Result<Boolean>) {
        if (result.code == StatusCode.FAILED) {
            setTaskStatus(StatusTask.TASK_EXCEPTION)
        } else setTaskStatus(StatusTask.TASK_FINISHED)

        taskControllerView.onTaskPrepared(result)

        updateTaskToRealm(mTaskBean)
        mRealmHelper.closeRealm()
    }


    private fun sendTaskResult() {
        mTaskStartCount--
        sendTaskMsg(0)

        updateTaskToRealm(mTaskBean)
    }


    private fun sendTaskMsg(delayTime: Long) {
        mHandler.sendEmptyMessageDelayed(MSG_TASK_COUNT, delayTime)
    }

    private fun dealError(msg: String) {
        L.d(msg)
        mTaskErrorCount++
        setTaskStatus(StatusTask.TASK_EXCEPTION)
    }

    private fun setTaskStatus(statusTask: StatusTask) {
        mTaskStatus = statusTask
        mTaskBean.task_status = statusTask.taskStatus
    }

    private fun updateTaskToRealm(taskBean: TaskBean) {
        mRealmHelper.insertTask(taskBean)
    }

}
