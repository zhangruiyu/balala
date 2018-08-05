package com.yuping.balala.ext

import com.yuping.balala.config.pgsqlConfig
import com.yuping.balala.config.redisConfig
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.auth.jwt.impl.JWTUser
import io.vertx.ext.web.RoutingContext
import io.vertx.redis.RedisClient

fun Vertx.initRedis(): RedisClient {
    return RedisClient.create(this, redisConfig)
}

fun Vertx.initPgsql(): AsyncSQLClient {
    return PostgreSQLClient.createShared(this, pgsqlConfig)
}

val log = LoggerFactory.getLogger("VertxExt")
fun RoutingContext.jwtUser(): JWTUser {
    val jwtUser = user() as JWTUser
    log.info("用户token信息:${jwtUser.principal()}")
    return jwtUser
}


