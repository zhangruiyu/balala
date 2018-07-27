package com.yuping.balala

import com.yuping.balala.config.commonRole
import com.yuping.balala.config.jwtConfig
import com.yuping.balala.router.SubRouterFactory
import com.yuping.balala.config.pgsqlConfig
import com.yuping.balala.config.port
import com.yuping.balala.ext.*
import com.yuping.balala.router.SubRouterFactoryImpl
import io.vertx.core.http.HttpServer
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.redis.RedisClient


class RestVerticle : CoroutineVerticle() {
    lateinit var postgreSQLClient: AsyncSQLClient
    lateinit var redis: RedisClient
    lateinit var subRouterFactory: SubRouterFactory
    override suspend fun start() {
        super.start()
        subRouterFactory = SubRouterFactoryImpl(JWTAuth.create(vertx, pgsqlConfig), vertx)
        postgreSQLClient = vertx.initPgsql()
        redis = vertx.initRedis()
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        val authProvider = JWTAuth.create(vertx, jwtConfig)
        router.routeWithRegex(".*/common/.*").handler(JWTAuthHandler.create(authProvider).addAuthorities(commonRole))
        router.mountSubRouter("/auth", subRouterFactory.create(SubRouterFactory.SubRouterType.AUTH))
//        router.get("/home").handler(listChain)
//        router.mountSubRouter("auth")
        awaitResult<HttpServer> {
            vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, it)
        }
    }


    override suspend fun stop() {
        postgreSQLClient.close()
        redis.close {}
        super.stop()
    }
}
