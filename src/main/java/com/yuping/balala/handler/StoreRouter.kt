package com.yuping.balala.handler

import com.yuping.balala.ext.coroutineHandler
import com.yuping.balala.ext.jsonOKNoData
import com.yuping.balala.router.SubRouter
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

class StoreRouter(vertx: Vertx) : SubRouter(vertx) {
    init {
        router.post("/storeManager/addRootType").coroutineHandler { ctx -> addRootType(ctx) }

    }

    /**
     * 添加店里项目根类型
     */
    private suspend fun addRootType(ctx: RoutingContext) {
        ctx.jsonOKNoData()
    }
}
