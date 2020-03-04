package com.task.cn.database

import com.task.cn.jbean.TaskBean

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
interface DBHelper {
    fun insertTask(taskBean: TaskBean)

    fun queryTasksByStatus(taskStatus: Int):List<TaskBean>

    fun queryTaskByTaskId(taskId:Long):TaskBean

    fun deleteTaskByStatus(taskStatus: Int)

    fun deleteAllTask()

    fun closeRealm()
}