package com.quin.backup.bean

import java.io.File

/**
 * Description:
 * Created by Quinin on 2020-01-15.
 **/
data class Result(var code: Int, var msg: String, var file: File?){
    companion object{
        const val SUCCEED_CODE = 200
        const val FAILED_CODE = 400
        const val CANCEL_CODE = 300
    }

    override fun toString(): String {
        return "Result(code=$code, msg='$msg', file=$file)"
    }

}