package com.task.cn.proxy

/**
 * Description:
 * Created by Quinin on 2020-03-05.
 **/
interface IProxy {
    fun startProxy(proxyRequestListener: ProxyRequestListener,cityName: String)
}