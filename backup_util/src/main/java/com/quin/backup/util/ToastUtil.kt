package com.quin.backup.util

import android.content.Context
import android.widget.Toast

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
class ToastUtil {
    companion object {
        fun showToast(context: Context, toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }

        fun showToast(context: Context, toast: String, duration: Int) {
            Toast.makeText(context, toast, duration).show()
        }
    }
}