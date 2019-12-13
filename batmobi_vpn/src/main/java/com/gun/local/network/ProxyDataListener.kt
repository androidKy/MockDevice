package com.gun.local.network
import com.gun.local.bean.ProxyIPBean


/**
 * Description:
 * Created by Quinin on 2019-08-16.
 **/
interface ProxyDataListener {
    fun onFailed(failedMsg: String)
    fun onResponProxyData(proxyIPBean: ProxyIPBean?)
}