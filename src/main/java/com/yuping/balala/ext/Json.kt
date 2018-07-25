package com.yuping.balala.ext

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

internal fun jsonResponse(response: HttpServerResponse, statusCode: Int, body: JsonObject) {
    response.statusCode = statusCode
    response.putHeader("content-type", "application/json; charset=utf-8")
        .end(body.toString())
}

//有返回值给客户端
fun RoutingContext.jsonOk(data: Any, code: Int = 200, msg: String = "") = jsonResponse(response(), 200, json {
    obj(
        "code" to code,
        "data" to data)
})

//没返回值 只有提示
fun RoutingContext.jsonOKNoData(msg: String = "", code: Int = 200) = jsonResponse(response(), 200, json {
    obj(
        "code" to code,
        "data" to msg)
})


//失败了
fun RoutingContext.jsonNormalFail(message: String, code: Int = 1001, data: Any? = null) = jsonResponse(response(), 200, json {
    obj(
        "code" to code,
        "message" to message,
        "data" to data)
})


