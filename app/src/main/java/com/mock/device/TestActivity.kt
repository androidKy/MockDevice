package com.mock.device

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.safframework.log.L
import com.task.cn.ProxyConstant
import com.task.cn.Result
import com.task.cn.StatusTask
import com.task.cn.jbean.IpInfoBean
import com.task.cn.manager.TaskManager
import com.task.cn.proxy.ProxyManager
import com.task.cn.proxy.ProxyRequestListener
import com.task.cn.task.ITaskControllerView
import com.utils.common.DevicesUtil
import com.utils.common.ToastUtils
import com.utils.common.Utils
import kotlinx.android.synthetic.main.activity_test.*

class TestActivity : AppCompatActivity(), View.OnClickListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        L.init("proxy_test")

        /* Thread {
             PingManager.verifyIP("广州市", object : VerifyIpListener {
                 override fun onVerifyIP(result: Boolean, verifyIpBean: VerifyIpBean?) {
                     L.d("thread name : ${Thread.currentThread().name}")
                     ToastUtils.showToast(this@TestActivity, "ip校验结果: $result")
                 }
             })
         }.start()*/

        bt_proxy.setOnClickListener(this)
        bt_task.setOnClickListener(this)


        //L.d("ip: ${DevicesUtil.getIPAddress(this.applicationContext)}")
        val localIp = DevicesUtil.getIPAddress(this.applicationContext)
        val proxyUrl = "${ProxyConstant.IP_URL}440900&ip=$localIp"
        AndroidNetworking.get(proxyUrl)
                .build()
                .getAsString(object : StringRequestListener {
                    override fun onResponse(response: String?) {
                        tv_tip.text = response
                    }

                    override fun onError(anError: ANError?) {
                        tv_tip.text = "代理请求失败"
                    }
                })
    }


    override fun onClick(v: View?) {
        val cityName = et_cityName.text.toString()
        when (v?.id!!) {
            R.id.bt_proxy -> {
                startProxy(cityName)
            }
            R.id.bt_task -> {
                startTask(cityName)
            }
        }
    }

    private fun startProxy(cityName: String) {
        ProxyManager()
                .setCityName(cityName)
                .setProxyRequestListener(object : ProxyRequestListener {
                    override fun onProxyResult(result: Result<IpInfoBean>) {
                        L.d("代理： $result")
                        ToastUtils.showToast(this@TestActivity, "代理: $result")
                        tv_tip.text = result.msg
                    }
                })
                .startProxy()
    }

    private fun startTask(cityName: String) {
        TaskManager.Companion.TaskBuilder()
                .setLastTaskStatus(StatusTask.TASK_FINISHED)
                .setAccountSwitch(false)
                .setDeviceSwitch(false)
                .setIpSwitch(true)
                .setTaskInfoSwitch(true)
                .setCityName(cityName)
                .setTaskControllerView(object : ITaskControllerView {
                    override fun onTaskPrepared(result: Result<Boolean>) {
                        //L.d("task end: $result")
                        L.d("任务： $result")
                        tv_tip.text = result.msg
                        ToastUtils.showToast(this@TestActivity, "任务: $result")
                    }
                })
                .build()
                .startTask()
    }
}
