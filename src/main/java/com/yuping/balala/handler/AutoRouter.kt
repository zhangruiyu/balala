package com.yuping.balala.handler

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.yuping.balala.config.Roles
import com.yuping.balala.config.jwtConfig
import com.yuping.balala.config.openIdTypeList
import com.yuping.balala.ext.*
import com.yuping.balala.router.SubRouter
import com.yuping.balala.utils.AuthCodeUtils
import com.yuping.balala.utils.AutoCode
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.sql.querySingleWithParamsAwait
import io.vertx.kotlin.ext.sql.updateWithParamsAwait
import io.vertx.kotlin.redis.getAwait


class AutoRouter(vertx: Vertx) : SubRouter(vertx) {

    private val authProvider = JWTAuth.create(vertx, jwtConfig)

    init {
//        router.route("/user/*").handler(JWTAuthHandler.create(authProvider).addAuthorities(commonRole))
        router.post("/register1").coroutineHandler { ctx -> register1(ctx) }
        router.post("/register2").coroutineHandler { ctx -> register2(ctx) }
        router.post("/login").coroutineHandler { ctx -> login(ctx) }
        router.post("/forgetPassword").coroutineHandler { ctx -> forgetPassword(ctx) }
        router.post("/common/bindOpenID").coroutineHandler { ctx -> bindOpenID(ctx) }
        router.post("/common/unBindOpenID").coroutineHandler { ctx -> unBindOpenID(ctx) }
    }

    private suspend fun forgetPassword(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        (tel.length < 11).yes {
            ctx.jsonNormalFail("手机号格式错误")
        }.otherwise {
            AuthCodeUtils.sendKeyCode(ctx, redis, AutoCode.ForgetPassword, tel)
        }
    }

    //根据手机号发送验证码
    private suspend fun register1(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        if (tel.length < 11) {
            ctx.jsonNormalFail("手机号格式错误")
        } else {
            val single = queryUserByType(tel, "tel")
            if (single != null) {
                ctx.jsonNormalFail("手机号已经注册")
            } else {
                AuthCodeUtils.sendKeyCode(ctx, redis, AutoCode.RegisterCode, tel)
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
            if (queryUserByType(tel, "tel") != null) {
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
        val identityType = ctx get ("identity_type" to "")
        if (identifier.isEmpty() || credential.isEmpty() || identityType.isEmpty()) {
            ctx.jsonNormalFail("输入信息不完整")
        } else {
            val userByIdentifier = queryUserByType(identifier, identityType)
            val autos = JsonArray(userByIdentifier?.getString(1))
            if (checkLogin(identifier, credential, identityType, autos)) {
                val telIdentifierInfo = queryIdentifierByType(autos, "tel")
                val tel = telIdentifierInfo!!.getString("identifier")
                val token = authProvider.generateToken(json {
                    obj("tel" to tel,
                        "id" to userByIdentifier!!.getInteger(0),
                        "role" to array(Roles.COMMON.name)
                    )
                })

                ctx.jsonOk(json {
                    obj(
                        "token" to token,
                        "tel" to tel
                    )
                })
            } else {
                ctx.jsonNormalFail("登录信息不匹配")
            }

        }
    }

    //绑定第三方登录
    private suspend fun bindOpenID(ctx: RoutingContext) {
        val identifier = ctx get ("identifier" to "")
        val credential = ctx get ("credential" to "")
        val identityType = ctx get ("identity_type" to "")
        (identifier.isEmpty() || credential.isEmpty() || (!openIdTypeList.contains(identityType))).yes {
            ctx.jsonNormalFail("输入信息不完整")
        }.otherwise {
            val tel = ctx.jwtUser().principal().getString("tel")
            if (tel.isEmpty()) {
                ctx.jsonNormalFail("接口非法访问")
            } else {
                //查询到用户autos 判断里面是否有第三方对应的identifier
                val autos = JsonArray(queryUserByType(tel, "tel")?.getString(1))
                //查询到了,//没有就添加进去
                (queryIdentifierByType(autos, identityType) == null).yes {
                    val result = pgsql.autoConnetctionRun {
                        it.updateWithParamsAwait("update users set autos = autos || ?::jsonb  where id = ?;", json {
                            array(obj("identity_type" to identityType, "identifier" to identifier, "credential" to credential).toString(), ctx.jwtUser().principal().getInteger("id"))
                        })
                    }
                    (result.updated == 1).yes {
                        //返回添加成功
                        ctx.jsonOKNoData("绑定成功")
                    }.otherwise {
                        ctx.jsonNormalFail("绑定失败,请再次尝试或者联系客服")
                    }

                }.otherwise {

                    ctx.jsonNormalFail("已经绑定过$identityType")
                }


            }
        }
    }

    //解绑第三方登录
    private suspend fun unBindOpenID(ctx: RoutingContext) {
        val identityType = ctx get ("identity_type" to "")
        ((!openIdTypeList.contains(identityType))).yes {
            ctx.jsonNormalFail("输入信息不完整")
        }.otherwise {
            val tel = ctx.getUserField<String>("tel")
            if (tel!!.isEmpty()) {
                ctx.jsonNormalFail("接口非法访问")
            } else {
                //查询到用户autos 判断里面是否有第三方对应的identifier
                val autos = JsonArray(queryUserByType(tel, "tel")?.getString(1))
                //查询到了就删除
                val singleAuto = queryIdentifierByType(autos, identityType)
                (singleAuto != null).yes {

                    val result = pgsql.autoConnetctionRun {
                        it.updateWithParamsAwait("update users set autos = autos - ? where id = ?;", json {
                            array(autos.indexOf(singleAuto).toString(), ctx.getUserField<Int>("id").toString())
                        })
                    }
                    log.error(result.toJson().toString())
                    (result.updated == 1).yes {
                        //返回添加成功
                        ctx.jsonOKNoData("解绑成功")
                    }.otherwise {
                        ctx.jsonNormalFail("解绑失败,请再次尝试或者联系客服")
                    }

                }.otherwise {

                    ctx.jsonNormalFail("还没有绑定过$identityType")
                }


            }
        }
    }

    /**
     * 根据验证类型查用户信息
     */
    private suspend fun queryUserByType(identifier: String, identity_type: String): JsonArray? {
        val info = pgsql.autoConnetctionRun {
            return@autoConnetctionRun it.querySingleWithParamsAwait("SELECT * FROM users WHERE users.autos @> ?::jsonb", json {
                array("${array(obj("identity_type" to identity_type, "identifier" to identifier))}")
            })
        }
        log.info(info?.toString())
        return info
    }

    /**
     * 判断用户账号,密码是否正确
     */
    private fun checkLogin(identifier: String, credential: String, identity_type: String, userByIdentifier: JsonArray?): Boolean {
        if (userByIdentifier == null) {
            return false
        }
        val userSingleAuto = queryIdentifierByType(userByIdentifier, identity_type)
        return userSingleAuto?.getString("credential") == credential && userSingleAuto.getString("identifier") == identifier
    }

    private fun queryIdentifierByType(autos: JsonArray, identity_type: String): JsonObject? {
        val singleAuto = autos.singleOrNull {
            JsonObject(it.toString()).getString("identity_type") == identity_type
        }
        return if (singleAuto == null) {
            null
        } else {
            JsonObject(singleAuto.toString())
        }
    }

}
