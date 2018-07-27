package com.yuping.balala.handler

import com.yuping.balala.config.Roles
import com.yuping.balala.ext.autoConnetctionRun
import com.yuping.balala.config.commonRole
import com.yuping.balala.ext.coroutineHandler
import com.yuping.balala.ext.get
import com.yuping.balala.ext.jsonNormalFail
import com.yuping.balala.ext.jsonOKNoData
import com.yuping.balala.ext.jsonOk
import com.yuping.balala.router.SubRouter
import io.vertx.core.Vertx
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.jwt.impl.JWTUser
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.sql.querySingleWithParamsAwait
import io.vertx.kotlin.redis.getAwait
import io.vertx.kotlin.redis.setWithOptionsAwait
import io.vertx.redis.op.SetOptions


class AutoRouter(vertx: Vertx) : SubRouter(vertx) {

    val config = JWTAuthOptions()
        .setKeyStore(KeyStoreOptions()
            .setPath("keystore.jceks")
            .setType("jceks")
            .setPassword("secret")
        ).setPermissionsClaimKey("role")

    val authProvider = JWTAuth.create(vertx, config)

    init {
        router.route("/user/*").handler(JWTAuthHandler.create(authProvider).addAuthorities(commonRole))
        router.post("/register1").coroutineHandler { ctx -> register1(ctx) }
        router.post("/register2").coroutineHandler { ctx -> register2(ctx) }
        router.post("/login").coroutineHandler { ctx -> login(ctx) }
        router.post("/user/ddddd").coroutineHandler { ctx -> dddd(ctx) }
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
                    val result = pgsql.autoConnetctionRun {
                        return@autoConnetctionRun it.querySingleWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON) RETURNING id", json {
                            array {
                                this.add(obj(
                                    "identity_type" to "tel",
                                    "identifier" to tel,
                                    "credential" to password

                                ).toString())
                                this.add(obj(
                                    "create_time" to System.currentTimeMillis(),
                                    "nickname" to "昵称",
                                    "avatar" to "http://img.wowoqq.com/allimg/180114/1-1P114104225.jpg",
                                    "birthday" to null,
                                    "gender" to '1'
                                ).toString()
                                )
                            }
                        })
                    }
                    print("zhuc")
                    //说明成功
                    if (result != null) {
                        val userId = result.getLong(0)
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
    private suspend fun login(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        val password = ctx get ("password" to "")
        if (tel.length < 11 || password.isEmpty()) {
            ctx.jsonNormalFail("输入信息不完整")
        } else {
            val token = authProvider.generateToken(json {
                obj("tel" to tel,
                    "role" to array(Roles.COMMON.name)
                )
            })
            ctx.jsonOk(json {
                obj(
                    "token" to token,
                    "tel" to tel
                )
            })
        }
//        connection.updateWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON)", JsonArray().add(ctx.bodyAsString).add(ctx.bodyAsString))
    }

    //注册,验证验证码
    private suspend fun dddd(ctx: RoutingContext) {
        println((ctx.user() as JWTUser).principal())
        ctx.jsonOKNoData()
    }

}
