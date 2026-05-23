package com.dbwp031.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(
    var issuer: String = "auth-service",
    var secret: String = "local-dev-secret-local-dev-secret-local-dev-secret",
)
