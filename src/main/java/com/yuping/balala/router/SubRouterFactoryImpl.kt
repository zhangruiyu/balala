package com.yuping.balala.router

import com.yuping.balala.handler.AutoRouter
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.JWTAuthHandler

/**
 * @author Leibniz.Hu
 * Created on 2017-10-31 10:23.
 */
class SubRouterFactoryImpl constructor(private val jwtProvider: JWTAuth, private val vert: Vertx) : SubRouterFactory {
    private val jwtHandler: Handler<RoutingContext>

    init {
        this.jwtHandler = JWTAuthHandler.create(jwtProvider)
    }

    override fun create(type: SubRouterFactory.SubRouterType): Router? {
        val router: SubRouter = when (type) {
            SubRouterFactory.SubRouterType.AUTH -> {
                AutoRouter(vert)
            }
            else -> AutoRouter(vert)
        }
        return router.subRouter()
    }

    companion object {

        fun of(jwtProvider: JWTAuth, vert: Vertx): SubRouterFactory {
            return SubRouterFactoryImpl(jwtProvider, vert)
        }
    }
}
