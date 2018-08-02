package com.yuping.balala.utils

import com.yuping.balala.ext.jsonNormalFail
import com.yuping.balala.ext.jsonOk
import com.yuping.balala.ext.yes
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.redis.getAwait
import io.vertx.kotlin.redis.setWithOptionsAwait
import io.vertx.redis.RedisClient
import io.vertx.redis.op.SetOptions

class AuthCodeUtils {
    companion object {

        suspend fun sendKeyCode(ctx: RoutingContext, redis: RedisClient, type: AutoCode, tel: String, responseResult: Boolean = true): Boolean {
            val codeKey = "$type$tel"
            val code: String? = redis.getAwait(codeKey)
            when (type) {
                AutoCode.ForgetPassword -> {

                }
                AutoCode.RegisterCode -> {

                }
                else -> {
                }
            }
            return if (code?.isNotEmpty() == true) {
                responseResult.yes { ctx.jsonNormalFail("验证码还未过期,请过期后重新尝试") }
                false
            } else {
                //发送验证码
                val setOptions = SetOptions()
                redis.setWithOptionsAwait(codeKey, "888888", setOptions.setEX(60 * 5))
                responseResult.yes {
                    ctx.jsonOk(json {
                        obj(
                            "message" to "验证码发送成功"
                        )
                    })
                }
                true
            }
        }
    }
}

enum class AutoCode {
    ForgetPassword,
    RegisterCode,
}
