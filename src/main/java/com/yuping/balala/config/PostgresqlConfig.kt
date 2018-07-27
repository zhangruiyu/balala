package com.yuping.balala.config

import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

val pgsqlConfig = json {
    obj(
        "username" to "zhangruiyu",
        "database" to "balala",
        "host" to "localhost",
        "port" to 5432
    )
}
