package com.backup.test

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private var TEST_PKG = WECHAT_PKG

    companion object {
        const val TIKTOK_PKG = "com.ss.android.ugc.aweme"
        const val WECHAT_PKG = "com.tencent.mm"
        const val HAND_FOOTBALL = "com.hand.foot"

        //const val TEST_PKG = WECHAT_PKG
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.bt_start_backup -> {
                TEST_PKG = et_pkg.text.toString()
                tv_pkg.text = TEST_PKG
                tv_tip.text = "正在开始备份..."
                DataController.instance
                        .setDataListener(object : DataListener {
                            override fun onFinished(result: Result) {
                                Log.d(com.quin.TAG, "backup: $result")
                                ToastUtil.showToast(this@MainActivity, result.toString())
                                tv_tip.text = "对${TEST_PKG}备份完成"
                            }
                        })
                        .backupData(this.applicationContext, TEST_PKG)
            }
            R.id.bt_restore_backup -> {
                TEST_PKG = et_pkg.text.toString()
                tv_pkg.text = TEST_PKG
                tv_tip.text = "正在恢复备份..."
                DataController.instance
                        .setDataListener(object : DataListener {
                            override fun onFinished(result: Result) {
                                Log.d(com.quin.TAG, "restore: $result")
                                ToastUtil.showToast(this@MainActivity, result.toString())
                                tv_tip.text = "对${TEST_PKG}还原完成"
                            }
                        })
                        .restoreData(this.applicationContext, TEST_PKG)
            }
            R.id.bt_clear_backup -> {
                TEST_PKG = et_pkg.text.toString()
                tv_pkg.text = TEST_PKG
                val file = DataSaveDir.getZipFile(TEST_PKG)
                if (file.exists())
                    file.delete()
            }
            R.id.bt_clear_app_info ->{
                TEST_PKG = et_pkg.text.toString()
                tv_pkg.text = TEST_PKG
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

            R.id.bt_wechat ->{
                et_pkg.setText(WECHAT_PKG)
            }

            R.id.bt_titok ->{
                et_pkg.setText(TIKTOK_PKG)
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
        bt_wechat.setOnClickListener(this)
        bt_titok.setOnClickListener(this)


        PermissionManager().closeSelinusFile(null)

        et_pkg.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tv_pkg.text = s?.toString()
            }

        })
    }
}
