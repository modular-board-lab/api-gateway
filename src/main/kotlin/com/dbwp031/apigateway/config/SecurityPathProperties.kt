package com.dbwp031.apigateway.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
data class SecurityPathProperties(
    var publicPaths: List<PublicPath> = emptyList(),
)

data class PublicPath(
    var method: String = "",
    var path: String = "",
)
