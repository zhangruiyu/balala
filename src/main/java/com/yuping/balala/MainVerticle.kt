package com.yuping.balala

import io.vertx.core.AbstractVerticle
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType

class MainVerticle : AbstractVerticle() {

    @Throws(Exception::class)
    override fun start() {
        /*   vertx.createHttpServer().requestHandler { req ->
               req.response()
                   .putHeader("content-type", "text/plain")
                   .end("Hello from Vert.x!")
           }.listen(8080)
           println("HTTP server started on port 8080")*/
        vertx.deployVerticle(RestVerticle())
//        ShiroAuth.create(vertx, ShiroAuthOptions().setType(ShiroAuthRealmType.LDAP))
    }
}
