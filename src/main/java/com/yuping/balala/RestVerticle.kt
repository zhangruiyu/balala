package com.yuping.balala

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject



class RestVerticle:AbstractVerticle {
    override fun start() {
        super.start()
        val postgreSQLClientConfig = JsonObject().put("host", "mypostgresqldb.mycompany")
        val postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig)
    }
}
