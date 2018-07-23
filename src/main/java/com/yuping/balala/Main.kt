package com.yuping.balala

import io.vertx.core.Vertx


fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(MainVerticle::class.java.name)
}
