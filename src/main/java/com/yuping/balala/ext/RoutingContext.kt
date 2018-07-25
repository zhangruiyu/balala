package com.yuping.balala.ext

import io.vertx.ext.web.RoutingContext

infix fun <T> RoutingContext.get(p: Pair<String, T>): T {
    val result: String? = request().getParam(p.first)
    return if (result == null) {
        return p.second
    } else
        when (p.second) {
            is Long -> result.toLong()
            is Int -> result.toInt()
            is String -> result.toString()
            else -> throw IllegalArgumentException("Unsupported type.")
        } as T
}
