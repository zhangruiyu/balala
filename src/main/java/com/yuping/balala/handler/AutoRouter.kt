package com.yuping.balala.handler

import com.yuping.balala.router.SubRouter
import com.yuping.balala.config.coroutineHandler
import com.yuping.balala.ext.get
import com.yuping.balala.ext.jsonNormalFail
import com.yuping.balala.ext.jsonOKNoData
import com.yuping.balala.ext.jsonOk
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.updateWithParamsAwait
import io.vertx.kotlin.redis.getAwait
import io.vertx.kotlin.redis.setWithOptionsAwait
import io.vertx.redis.op.SetOptions

class AutoRouter(vertx: Vertx) : SubRouter(vertx) {
    init {
        router.post("/register1").coroutineHandler { ctx -> register1(ctx) }
        router.post("/register2").coroutineHandler { ctx -> register2(ctx) }
        router.post("/register3").coroutineHandler { ctx -> register3(ctx) }
    }

    //根据手机号发送验证码
    private suspend fun register1(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        if (tel.length < 11) {
            ctx.jsonNormalFail("手机号格式错误")
        } else {
            val codeKey = "RegisterCode$tel"
            val code: String? = redis.getAwait(codeKey)
            if (code?.isNotEmpty() == true) {

                //已发送直接跳转页面
//                ctx.jsonNormalFail("验证码发送成功")
                ctx.jsonNormalFail("验证码还未过期,请过期后重新尝试")
            } else {
                //发送验证码
                val setOptions = SetOptions()
                redis.setWithOptionsAwait(codeKey, "888888", setOptions.setEX(60 * 5))
                ctx.jsonOk(json {
                    obj(
                        "message" to "验证码发送成功"
                    )
                })
            }
        }
    }

    //注册,验证验证码
    private suspend fun register2(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        val password = ctx get ("password" to "")
        val authCode = ctx get ("authCode" to "")
        if (tel.length < 11 || password.isEmpty() || authCode.isEmpty()) {
            ctx.jsonNormalFail("手机号,密码和验证码不能不填")
        } else {
            val codeKey = "RegisterCode$tel"
            val code: String? = redis.getAwait(codeKey)
            //没有发送
            if (code?.isNotEmpty() == true) {
                if (authCode == code) {
                    //注册
                    val connection = pgsql.getConnectionAwait()
                    val result = connection.updateWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON) RETURNING id", json {
                        array {
                            add(obj(
                                "identity_type" to "tel",
                                "identifier" to tel,
                                "credential" to password

                            ).toString())
                            add(obj(
                                "create_time" to System.currentTimeMillis(),
                                "nickname" to "昵称",
                                "avatar" to "http://img.wowoqq.com/allimg/180114/1-1P114104225.jpg",
                                "birthday" to null,
                                "gender" to '1'
                            ).toString()
                            )
                        }
                    })
                    print(result.toJson())
                    //说明成功
                    if (result.updated == 1) {
                        ctx.jsonOKNoData()
                    } else {
                        ctx.jsonNormalFail("注册失败,请联系客服")
                    }
                } else {
                    ctx.jsonNormalFail("验证码有误,请重新尝试")
                }
            } else {
                ctx.jsonNormalFail("请返回上一步发送验证码")

            }

        }
    }

    //注册,验证验证码
    private suspend fun register3(ctx: RoutingContext) {
        val connection = pgsql.getConnectionAwait()
        connection.updateWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON)", JsonArray().add(ctx.bodyAsString).add(ctx.bodyAsString))
    }

}
