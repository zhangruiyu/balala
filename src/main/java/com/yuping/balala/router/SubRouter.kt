package com.yuping.balala.router

import com.yuping.balala.ext.initPgsql
import com.yuping.balala.ext.initRedis
import io.vertx.core.Vertx
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.redis.RedisClient

/**
 * 子路由的接口
 *
 * @author Leibniz.Hu
 * Created on 2017-10-12 10:11.
 */
abstract class SubRouter(val vertx: Vertx) {
    val router: Router = Router.router(vertx)
    val redis: RedisClient = vertx.initRedis()
    val pgsql: AsyncSQLClient = vertx.initPgsql()

    init {

    }

    /**
     * 获取子路由Router对象
     * 用于Router.mountSubRouter()绑定子路由
     */
    fun subRouter(): Router {
        return router
    }

}
