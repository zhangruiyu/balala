package com.yuping.balala.config

import io.vertx.kotlin.redis.RedisOptions


val redisConfig by lazy {
    val config = RedisOptions(host = "127.0.0.1", port = 6379)
    config
}
