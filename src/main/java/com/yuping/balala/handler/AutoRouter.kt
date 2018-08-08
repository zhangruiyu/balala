package com.yuping.balala.handler

import cn.hutool.core.date.DateUtil
import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException
import com.yuping.balala.config.Roles
import com.yuping.balala.config.jwtConfig
import com.yuping.balala.config.openIdTypeList
import com.yuping.balala.ext.*
import com.yuping.balala.router.SubRouter
import com.yuping.balala.utils.AuthCodeUtils
import com.yuping.balala.utils.AutoCode
import com.yuping.balala.utils.ValueCheckUtils
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
        router.post("/restPassword").coroutineHandler { ctx -> restPassword(ctx) }
        router.post("/common/bindOpenID").coroutineHandler { ctx -> bindOpenID(ctx) }
        router.post("/common/unBindOpenID").coroutineHandler { ctx -> unBindOpenID(ctx) }
        router.post("/common/updateUserInfo").coroutineHandler { ctx -> updateUserInfo(ctx) }
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
                            it.querySingleWithParamsAwait("INSERT INTO users (autos,avatar,create_time) VALUES (?::JSON,?,?) RETURNING id", json {
                                array(array(obj(
                                    "identity_type" to "tel",
                                    "identifier" to tel,
                                    "credential" to password

                                )).toString(), "http://img.wowoqq.com/allimg/180114/1-1P114104225.jpg", DateUtil.now())
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
            "输入信息不完整".throwMessageException()
        } else {
            val user = queryUserByType(identifier, identityType)
            val autos = getUserAutos(user!!)
            if (checkLogin(identifier, credential, identityType, autos)) {
                val telIdentifierInfo = queryAutoByType(autos, "tel")
                val tel = telIdentifierInfo!!.getString("identifier")
                val token = authProvider.generateToken(json {
                    obj("tel" to tel,
                        "id" to getUserId(user),
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
                (queryAutoByType(autos, identityType) == null).yes {
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
                val singleAuto = queryAutoByType(autos, identityType)
                (singleAuto != null).yes {
                    val result = pgsql.autoConnetctionRun {
                        it.updateWithParamsAwait("update users set autos = autos - ?::Int where id = ?;", json {
                            array(autos.indexOf(singleAuto).toString(), ctx.getUserField<Int>("id").toString())
                        })
                    }
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

    //发送重置密码的验证码
    private suspend fun forgetPassword(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        (tel.length < 11).yes {
            ctx.jsonNormalFail("手机号格式错误")
        }.otherwise {
            AuthCodeUtils.sendKeyCode(ctx, redis, AutoCode.ForgetPassword, tel)
        }
    }

    //重置密码
    private suspend fun restPassword(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        val code = ctx get ("code" to "")
        val newPassword = ctx get ("newPassword" to "")
        if (code.length < 6) {
            ctx.jsonNormalFail("验证码格式不对")
        } else if (!ValueCheckUtils.isTel(tel)) {
            ctx.jsonNormalFail("手机号格式不对")
        } else if (!ValueCheckUtils.isPassword(newPassword)) {
            ctx.jsonNormalFail("新密码格式不对")
        } else {
            val sendCode: String? = redis.getAwait("${AutoCode.ForgetPassword}$tel")
            //如果验证码没错
            when {
                sendCode == null -> ctx.jsonNormalFail("请发送验证码后再试")
                sendCode != code -> ctx.jsonNormalFail("验证码错误")
                else -> {
                    val user = queryUserByType(tel, "tel")
                    if (user == null) {
                        ctx.jsonNormalFail("该用户未注册")
                    } else {
                        val userId = getUserId(user)
                        val telIndex = querySingleAutoIndexById(getUserAutos(user), "tel")
                        val result = pgsql.autoConnetctionRun {
                            /*    it.updateWithParamsAwait("UPDATE users SET autos = jsonb_set(autos,'{$telIndex,credential}', '\"$newPassword\"', FALSE) WHERE id = ?", json {
                                    array(userId.toString())
                                })
                                */
                            it.updateWithParamsAwait("UPDATE users SET autos = jsonb_set(autos,array[?::text,'credential'::text], '\"hhhh\"', FALSE) WHERE id = ?;", json {
                                array(telIndex, userId.toString())
                            })
                        }
                        //说明成功
                        if (result.updated == 1) {
                            log.debug("注册成功,手机号$tel")
                            ctx.jsonOKNoData()
                        } else {
                            ctx.jsonNormalFail("密码修改失败,请联系客服")
                        }
                    }

                }
            }
        }

    }

    //重置密码
    private suspend fun updateUserInfo(ctx: RoutingContext) {
        val updateList = arrayListOf<Pair<String, String>>()
        val avatar = ctx get ("avatar" to "")
        val gender = ctx get ("gender" to "")
        val nickname = ctx get ("nickname" to "")
        val birthday = ctx get ("birthday" to "")
        if (avatar.isNotEmpty()) {
            updateList.add("avatar" to avatar)
        }
        if (gender.isNotEmpty()) {
            if (gender == "0" || gender == "1") {
                updateList.add("gender" to gender)
            } else {
                "性别格式不对".throwMessageException()
            }
        }
        if (nickname.isNotEmpty()) {
            updateList.add("nickname" to nickname)
        }
        if (birthday.isNotEmpty()) {
            updateList.add("birthday" to nickname)
        }
        if (updateList.isEmpty()) {
            "请添加需要修改的数据".throwMessageException()
        }
        val names = updateList.joinToString("=?,", postfix = "=?,") { it.first }.removeSuffix(",")
        val values = (updateList.map {
            it.second
        }).toList().toMutableList()
        values.add(getUserId(ctx.principal()).toString())
        val result = pgsql.autoConnetctionRun {
            it.updateWithParamsAwait("UPDATE users SET $names WHERE id = ?", json {
                array(values)
            })
        }
        //说明成功
        if (result.updated == 1) {
            ctx.jsonOKNoData()
        } else {
            ctx.jsonNormalFail("资料修改失败")
        }
    }

    /**
     * 判断用户账号,密码,类型是否正确
     */
    private fun checkLogin(identifier: String, credential: String, identity_type: String, userByIdentifier: JsonArray?): Boolean {
        if (userByIdentifier == null) {
            return false
        }
        val userSingleAuto = queryAutoByType(userByIdentifier, identity_type)
        return userSingleAuto?.getString("credential") == credential && userSingleAuto.getString("identifier") == identifier
    }

    /**
     * 根据所以autos查询到对应type的单一对象
     */
    private fun queryAutoByType(autos: JsonArray, identity_type: String): JsonObject? {
        val singleAuto = autos.singleOrNull {
            JsonObject(it.toString()).getString("identity_type") == identity_type
        }
        return if (singleAuto == null) {
            null
        } else {
            JsonObject(singleAuto.toString())
        }
    }

    /**
     * 根据所以userId查询到对应type的单一对象
     */
    private suspend fun queryAutosById(id: Int): JsonArray? {
        return pgsql.autoConnetctionRun {
            return@autoConnetctionRun it.querySingleWithParamsAwait("SELECT users.autos FROM users WHERE users.id = ?;", json {
                array(id)
            })
        }
    }


    /**
     * 根据autos和identity_type查询到对应auto的index
     */
    private fun querySingleAutoIndexById(autos: JsonArray, identity_type: String): Int {
        return autos.indexOf(queryAutoByType(autos, identity_type))
    }

    /**
     * 根据所以userId查询到对应user
     */
    private suspend fun queryUserById(id: Int): JsonArray? {
        return pgsql.autoConnetctionRun {
            return@autoConnetctionRun it.querySingleWithParamsAwait("SELECT users.id,users.autos FROM users WHERE users.id = ?;", json {
                array(id)
            })
        }
    }

    /**
     * 根据验证类型查用户信息
     */
    private suspend fun queryUserByType(identifier: String, identity_type: String): JsonArray? {
        val info = pgsql.autoConnetctionRun {
            return@autoConnetctionRun it.querySingleWithParamsAwait("SELECT users.id,users.autos FROM users WHERE users.autos @> ?::jsonb", json {
                array("${array(obj("identity_type" to identity_type, "identifier" to identifier))}")
            })
        }
        log.info(info?.toString())
        return info
    }

    /**
     * 根据user拿到autos
     */
    private fun getUserAutos(user: JsonArray): JsonArray {
        return JsonArray(user.getString(1))
    }

    /**
     * 根据userarray拿到id
     */
    private fun getUserId(user: JsonArray): Int {
        return user.getInteger(0)
    }

    /**
     * 根据userobj(也就是token里)拿到id
     */
    private fun getUserId(user: JsonObject): Int {
        return user.getInteger("id")
    }
}
