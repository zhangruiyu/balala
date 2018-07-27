package com.yuping.balala.config

import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuthOptions

val jwtConfig = JWTAuthOptions()
    .setKeyStore(KeyStoreOptions()
        .setPath("keystore.jceks")
        .setType("jceks")
        .setPassword("secret")
    ).setPermissionsClaimKey("role")
