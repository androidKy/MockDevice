package com.quin.backup.util

import android.content.Context
import java.lang.ref.WeakReference

/**
 * Description:
 * Created by Quinin on 2020-01-18.
 **/
class ContextUtil {
    companion object {
        private var mWRcontext: WeakReference<Context>? = null

        fun setContext(context: Context) {
            mWRcontext = WeakReference(context)
        }

        fun getContext(): Context? {
            return mWRcontext?.get()
        }

        fun clear() {
            mWRcontext?.clear()
        }
    }
}