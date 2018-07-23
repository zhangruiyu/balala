package com.yuping.balala.config


val port by lazy {
    System.getProperty("port")?.toInt() ?: 8080
}
