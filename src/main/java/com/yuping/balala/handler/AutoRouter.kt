package com.yuping.balala.handler

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.yuping.balala.config.Roles
import com.yuping.balala.config.jwtConfig
import com.yuping.balala.ext.*
import com.yuping.balala.router.SubRouter
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.impl.JWTUser
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.*
import io.vertx.kotlin.ext.sql.querySingleWithParamsAwait
import io.vertx.kotlin.redis.getAwait
import io.vertx.kotlin.redis.setWithOptionsAwait
import io.vertx.redis.op.SetOptions


class AutoRouter(vertx: Vertx) : SubRouter(vertx) {


    private val authProvider = JWTAuth.create(vertx, jwtConfig)

    init {
//        router.route("/user/*").handler(JWTAuthHandler.create(authProvider).addAuthorities(commonRole))
        router.post("/register1").coroutineHandler { ctx -> register1(ctx) }
        router.post("/register2").coroutineHandler { ctx -> register2(ctx) }
        router.post("/login").coroutineHandler { ctx -> login(ctx) }
        router.post("/common/ddddd").coroutineHandler { ctx -> dddd(ctx) }
    }

    //根据手机号发送验证码
    private suspend fun register1(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        if (tel.length < 11) {
            ctx.jsonNormalFail("手机号格式错误")
        } else {
            val single = queryUserByTel(tel, "tel")
            if (single != null) {
                ctx.jsonNormalFail("手机号已经注册")
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
    }

    //注册,验证验证码
    private suspend fun register2(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        val password = ctx get ("password" to "")
        val authCode = ctx get ("authCode" to "")
        if (tel.length < 11 || password.isEmpty() || authCode.isEmpty()) {
            ctx.jsonNormalFail("手机号,密码和验证码不能不填")
        } else {
            if (queryUserByTel(tel, "tel") != null) {
                ctx.jsonNormalFail("手机号已经注册")
                return
            }
            val codeKey = "RegisterCode$tel"
            val code: String? = redis.getAwait(codeKey)
            //没有发送
            if (code?.isNotEmpty() == true) {
                if (authCode == code) {
                    //注册
                    try {
                        val result = pgsql.autoConnetctionRun {
                            it.querySingleWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON) RETURNING id", json {
                                array {
                                    this.add("[${(obj(
                                        "identity_type" to "tel",
                                        "identifier" to tel,
                                        "credential" to password

                                    ))}]")
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
                        //说明成功
                        if (result != null) {
                            log.debug("注册成功,手机号$tel")
                            val userId = result.getLong(0)
                            ctx.jsonOKNoData()
                        } else {
                            ctx.jsonNormalFail("注册失败,请联系客服")
                        }
                    } catch (e: GenericDatabaseException) {
                        ctx.jsonNormalFail("手机号已经注册")
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
//        identifier: String, credential: String, identity_type: String
        val identifier = ctx get ("identifier" to "")
        val credential = ctx get ("credential" to "")
        val identity_type = ctx get ("identity_type" to "")
        if (identifier.isEmpty() || credential.isEmpty() || identity_type.isEmpty()) {
            ctx.jsonNormalFail("输入信息不完整")
        } else {
            val userByIdentifier = queryUserByTel(identifier, identity_type)

//            val mapFrom = JsonObject.mapFrom(userByTel?.getString(1))
            if (checkLogin(identifier, credential, identity_type, JsonArray(userByIdentifier?.getString(1)))) {
                val token = authProvider.generateToken(json {
                    obj("tel" to "",
                        "role" to array(Roles.COMMON.name)
                    )
                })
                ctx.jsonOk(json {
                    obj(
                        "token" to token,
                        "tel" to ""
                    )
                })
            } else {
                ctx.jsonNormalFail("登录信息不匹配")
            }

        }
//        connection.updateWithParamsAwait("INSERT INTO users (autos, info) VALUES (?::JSON,?::JSON)", JsonArray().add(ctx.bodyAsString).add(ctx.bodyAsString))
    }

    //注册,验证验证码
    private suspend fun dddd(ctx: RoutingContext) {
        println((ctx.user() as JWTUser).principal())
        ctx.jsonOKNoData()
    }

    /**
     * 根据验证类型查用户信息
     */
    private suspend fun queryUserByTel(identifier: String, identity_type: String): JsonArray? {
        val info = pgsql.autoConnetctionRun {
            return@autoConnetctionRun it.querySingleWithParamsAwait("SELECT * FROM users WHERE users.autos @> ?::jsonb", json {
                array("${array(obj("identity_type" to identity_type, "identifier" to identifier))}")
            })
        }
        log.info(info?.toString())
        return info
    }

    private fun checkLogin(identifier: String, credential: String, identity_type: String, userByIdentifier: JsonArray?): Boolean {
        if (userByIdentifier == null) {
            return false
        }
        val userSingleAuto = io.vertx.core.json.JsonObject(userByIdentifier.single {
            io.vertx.core.json.JsonObject(it.toString()).getString("identity_type") == identity_type
        }.toString())
        return userSingleAuto.getString("credential") == credential && userSingleAuto.getString("identifier") == identifier
    }

    private fun checkPassword(localPassword: String, inputPassword: String): Boolean {
        return localPassword == inputPassword
    }

}
