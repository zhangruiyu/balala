package com.yuping.balala.ext

import com.yuping.balala.exception.MessageException
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.sql.getConnectionAwait
import kotlinx.coroutines.experimental.launch

/**
 * An extension method for simplifying coroutines usage with Vert.x Web routers
 */
fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
        launch(ctx.vertx().dispatcher()) {
            try {
                fn(ctx)
            } catch (e: MessageException) {
                ctx.jsonNormalFail(e.message!!, e.code, e.data)
            } catch (e: Exception) {
                ctx.fail(e)
            }
        }
    }
}

suspend fun <T> AsyncSQLClient.autoConnetctionRun(block: suspend (SQLConnection) -> T): T {
    val connection = getConnectionAwait()
    val result = block(connection)
    connection.close()
    return result
}
