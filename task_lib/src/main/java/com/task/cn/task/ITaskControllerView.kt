package com.task.cn.task

import com.task.cn.Result

/**
 * Description:
 * Created by Quinin on 2020-03-04.
 **/
interface ITaskControllerView {
    fun onTaskPrepared(result: Result<Boolean>)
}