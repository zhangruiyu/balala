package com.yuping.balala

import com.yuping.balala.config.pgsqlConfig
import com.yuping.balala.config.port
import com.yuping.balala.ext.jsonResponse
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle


class RestVerticle : CoroutineVerticle() {
    lateinit var postgreSQLClient: AsyncSQLClient
    var x = 0
    override suspend fun start() {
        super.start()
        postgreSQLClient = PostgreSQLClient.createShared(vertx, pgsqlConfig)
        val router = Router.router(vertx)

        router.get("/login").handler(listChain)

        val server = vertx.createHttpServer()
        server.requestHandler(router::accept).listen(port)
    }

    private val listChain = { rc: RoutingContext ->
        val body = json {
            obj(
                "x" to x,
                "b" to "123")
        }
        for (y in 1..55) {
            x++
        }

        jsonResponse(rc.response(), 200, body)
    }

    override suspend fun stop() {
        postgreSQLClient.close()
        super.stop()
    }
}
