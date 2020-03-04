package com.task.cn.task

import com.task.cn.manager.TaskBuilder

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
interface ITaskController {
    fun startTask(taskBuilder: TaskBuilder)
}