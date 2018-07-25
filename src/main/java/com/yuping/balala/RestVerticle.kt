package com.yuping.balala

import com.yuping.balala.config.coroutineHandler
import com.yuping.balala.config.pgsqlConfig
import com.yuping.balala.config.port
import com.yuping.balala.config.redisConfig
import com.yuping.balala.ext.get
import com.yuping.balala.ext.jsonNormalFail
import com.yuping.balala.ext.jsonOk
import io.vertx.core.http.HttpServer
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.redis.getAwait
import io.vertx.kotlin.redis.setAwait
import io.vertx.kotlin.redis.setWithOptionsAwait
import io.vertx.redis.RedisClient
import io.vertx.redis.op.SetOptions
import kotlinx.coroutines.experimental.launch


class RestVerticle : CoroutineVerticle() {
    lateinit var postgreSQLClient: AsyncSQLClient
    lateinit var redis: RedisClient
    var x = 0
    override suspend fun start() {
        super.start()
        postgreSQLClient = PostgreSQLClient.createShared(vertx, pgsqlConfig)
        redis = RedisClient.create(vertx, redisConfig)
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.post("/public/auth/register1").coroutineHandler { ctx -> register1(ctx) }
        router.post("/public/auth/register2").coroutineHandler { ctx -> register2(ctx) }
//        router.get("/home").handler(listChain)
//        router.mountSubRouter("auth")
        awaitResult<HttpServer> {
            vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config.getInteger("http.port", port), it)
        }
    }

    //根据手机号发送验证码
    private suspend fun register1(ctx: RoutingContext) {
        val tel = ctx get ("tel" to "")
        if (tel.length < 11) {
            ctx.jsonNormalFail("手机号格式错误")
        } else {
            val codeKey = "RegisterCode$tel"
            for (i in 1..200000) {
                val code: String = redis.getAwait(codeKey)
            }
            val code: String? = redis.getAwait(codeKey)
            if (code?.isEmpty() == true) {
                //发送验证码
                val setOptions = SetOptions()
                redis.setWithOptionsAwait(codeKey, "888888", setOptions.setEX(60 * 5))
            } else {
                //已发送直接跳转页面
//                ctx.jsonNormalFail("验证码发送成功")
            }
            ctx.jsonOk(json {
                obj(
                    "message" to "验证码发送成功"
                )
            })
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
            for (i in 1..10000) {
                val code: String = redis.getAwait(codeKey)
            }
            val code: String? = redis.getAwait(codeKey)
            if (code?.isEmpty() == true) {
                //发送验证码
                val setOptions = SetOptions()
                redis.setWithOptionsAwait(codeKey, "888888", setOptions.setEX(60 * 5))
            } else {
                //已发送直接跳转页面
//                ctx.jsonNormalFail("验证码发送成功")
            }
            ctx.jsonOk(json {
                obj(
                    "message" to "验证码发送222成功"
                )
            })
        }
    }

    override suspend fun stop() {
        postgreSQLClient.close()
        redis.close {}
        super.stop()
    }
}
