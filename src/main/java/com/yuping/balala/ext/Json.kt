package com.yuping.balala.ext

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject

internal fun jsonResponse(response: HttpServerResponse, statusCode: Int, body: JsonObject) {
    response.statusCode = statusCode
    response.putHeader("content-type", "application/json; charset=utf-8")
        .end(body.toString())
}
