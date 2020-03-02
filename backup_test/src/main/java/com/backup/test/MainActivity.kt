package com.backup.test

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.quin.backup.bean.Result
import com.quin.backup.controller.DataController
import com.quin.backup.dir.DataSaveDir
import com.quin.backup.listener.DataListener
import com.quin.backup.permission.PermissionManager
import com.quin.backup.util.CommandUtil
import com.quin.backup.util.ThreadUtils
import com.quin.backup.util.ToastUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TIKTOK_PKG = "com.ss.android.ugc.aweme"
        const val WECHAT_PKG = "com.tencent.mm"
        const val HAND_FOOTBALL = "com.hand.foot"

        const val TEST_PKG = TIKTOK_PKG
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.bt_start_backup -> {
                DataController.instance
                        .setDataListener(object : DataListener {
                            override fun onFinished(result: Result) {
                                Log.d(com.quin.TAG, "backup: $result")
                                ToastUtil.showToast(this@MainActivity, result.toString())
                            }
                        })
                        .backupData(this.applicationContext, TEST_PKG)
            }
            R.id.bt_restore_backup -> {
                DataController.instance
                        .setDataListener(object : DataListener {
                            override fun onFinished(result: Result) {
                                Log.d(com.quin.TAG, "restore: $result")
                                ToastUtil.showToast(this@MainActivity, result.toString())
                            }
                        })
                        .restoreData(this.applicationContext, TEST_PKG)
            }
            R.id.bt_clear_backup -> {
                val file = DataSaveDir.getZipFile(TEST_PKG)
                if (file.exists())
                    file.delete()
            }
            R.id.bt_clear_app_info ->{
                ThreadUtils.executeByCached(object:ThreadUtils.Task<Boolean>(){
                    override fun doInBackground(): Boolean {
                        var result = false

                        CommandUtil.sendCommand("rm -fr $TEST_PKG",object :CommandUtil.OnResponListener{
                            override fun onSuccess(responList: MutableList<String>?) {
                                result = true
                            }

                            override fun onFailed(msg: String?) {
                            }
                        })

                        return result
                    }

                    override fun onSuccess(result: Boolean) {
                        if(result)
                            ToastUtil.showToast(this@MainActivity,"$TEST_PKG 包名下的信息已清除")
                    }

                    override fun onCancel() {
                    }

                    override fun onFail(t: Throwable?) {
                    }

                })
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bt_start_backup.setOnClickListener(this)
        bt_restore_backup.setOnClickListener(this)
        bt_clear_backup.setOnClickListener(this)
        bt_clear_app_info.setOnClickListener(this)

        PermissionManager().closeSelinusFile(null)
    }
}
