package com.yuping.balala.ext

import com.yuping.balala.exception.MessageException
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLClient
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.awaitResult
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.sql.getConnectionAwait
import io.vertx.kotlin.ext.sql.queryWithParamsAwait
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

suspend fun SQLConnection.querySingleObjWithParamsAwait(sql: String, arguments: JsonArray): JsonObject? {
    val resultSet = queryWithParamsAwait(sql, arguments)
    if (resultSet.rows.isEmpty()) {
        return null
    }
    return resultSet.rows[0]
}
