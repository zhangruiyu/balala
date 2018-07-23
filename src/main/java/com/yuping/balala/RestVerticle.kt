package com.yuping.balala

import com.yuping.balala.config.pgsqlConfig
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.CoroutineVerticle


class RestVerticle : CoroutineVerticle() {
    lateinit var postgreSQLClient: AsyncSQLClient
    override suspend fun start() {
        super.start()
        postgreSQLClient = PostgreSQLClient.createShared(vertx, pgsqlConfig)
        val router = Router.router(vertx)

        router.route().handler { routingContext ->

            // This handler will be called for every request
            val response = routingContext.response()
            response.putHeader("content-type", "text/plain")

            // Write to the response and end it
            response.end("Hello World from Vert.x-Web!")
        }

        val server = vertx.createHttpServer()
        server.requestHandler(router::accept).listen(8080)
    }

    override suspend fun stop() {
        postgreSQLClient.close()
        super.stop()
    }
}
