package com.task.cn.proxy

import com.task.cn.Result
import com.task.cn.jbean.IpInfoBean

/**
 * Description:
 * Created by Quinin on 2020-03-05.
 **/
interface ProxyRequestListener {
    fun onProxyResult(result: Result<IpInfoBean>)
}