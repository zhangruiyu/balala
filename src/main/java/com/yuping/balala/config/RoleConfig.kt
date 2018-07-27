package com.yuping.balala.config

enum class Roles {
    COMMON,
    ADMIN
}

val commonRole = setOf(Roles.COMMON.name)
val adminRole = setOf(Roles.COMMON.name, Roles.ADMIN.name)
