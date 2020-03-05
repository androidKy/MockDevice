package com.task.cn.proxy

import com.safframework.log.L
import com.task.cn.Result
import com.task.cn.StatusCode
import com.task.cn.StatusMsg
import com.task.cn.jbean.IpInfoBean
import com.task.cn.jbean.VerifyIpBean
import com.utils.common.ToastUtils

/**
 * Description:
 * Created by Quinin on 2020-03-05.
 **/
class ProxyManager {
    private var mCityName: String? = null
    private var mProxyRequestListener: ProxyRequestListener? = null

    fun setCityName(cityName: String?): ProxyManager {
        mCityName = cityName
        return this
    }

    fun setProxyRequestListener(proxyRequestListener: ProxyRequestListener?): ProxyManager {
        mProxyRequestListener = proxyRequestListener

        return this
    }

    fun startProxy() {
        if (mCityName.isNullOrEmpty()) {
            L.d("cityName can not be null")
            //ToastUtils.showToast("城市名不能为空")
            mProxyRequestListener?.onProxyResult(Result(StatusCode.FAILED, IpInfoBean(), "cityName can not be null"))
            return
        }
        if (mProxyRequestListener == null) {
            L.d("mProxyRequestListener can not be null")
            //ToastUtils.showToast("mProxyRequestListener不能为空")
            mProxyRequestListener?.onProxyResult(Result(StatusCode.FAILED, IpInfoBean(), "mProxyRequestListener can not be null"))
            return
        }

        ProxyController().startProxy(object : ProxyRequestListener {
            override fun onProxyResult(result: Result<IpInfoBean>) {
                if (result.code == StatusCode.FAILED)
                    mProxyRequestListener?.onProxyResult(result)
                else {
                    //校验切换后的IP是否正确
                    PingManager.verifyIP(mCityName!!, object : VerifyIpListener {
                        override fun onVerifyIP(result: Boolean, verifyIpBean: VerifyIpBean?) {
                            if (result) {
                                val ipBean = IpInfoBean()
                                ipBean.city = mCityName
                                ipBean.ip = verifyIpBean?.cip
                                mProxyRequestListener?.onProxyResult(Result(StatusCode.SUCCEED, ipBean, StatusMsg.SUCCEED.msg))
                            } else {
                                mProxyRequestListener?.onProxyResult(Result(StatusCode.FAILED, IpInfoBean(), "校验IP失败"))
                            }
                        }
                    })
                }
            }
        }, mCityName!!)
    }
}