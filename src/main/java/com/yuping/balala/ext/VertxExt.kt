package com.yuping.balala.ext

import com.yuping.balala.config.pgsqlConfig
import com.yuping.balala.config.redisConfig
import io.vertx.core.Vertx
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.redis.RedisClient

fun Vertx.initRedis(): RedisClient {
    return RedisClient.create(this, redisConfig)
}

fun Vertx.initPgsql(): AsyncSQLClient {
    return PostgreSQLClient.createShared(this, pgsqlConfig)
}
