package com.yuping.balala.config

enum class Roles {
    COMMON,//登录
    STORE_NORMAL,//普通店员
    STORE_MANAGER,//店长
    ADMIN//最屌管理员
}

val commonRole = mutableSetOf(Roles.COMMON.name)
val storeNormalRole = commonRole.addRole(Roles.STORE_NORMAL)
val storeManagerRole = storeNormalRole.addRole(Roles.STORE_MANAGER)
val adminRole = storeManagerRole.addRole(Roles.COMMON)

fun MutableSet<String>.addRole(vararg elements: Roles): MutableSet<String> {
    return apply {
        elements.forEach {
            add(it.name)
        }
    }
}
