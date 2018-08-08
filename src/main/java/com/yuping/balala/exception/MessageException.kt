package com.yuping.balala.exception

import com.yuping.balala.config.NORMAL_ERROR_CODE

/**
 * Created by zhangruiyu on 2017/5/24.
 */
class MessageException : RuntimeException {
    var code = NORMAL_ERROR_CODE
    var data: Any? = null

    constructor() {}

    constructor(message: String, code: Int = NORMAL_ERROR_CODE, data: Any? = null) : super(message) {
        this.code = code
        this.data = data
    }

    //    constructor(message: String, cause: Throwable) : super(message, cause) {}
//
//    constructor(cause: Throwable) : super(cause) {}
//
//    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace) {}
}
