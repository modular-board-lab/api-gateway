package com.dbwp031.apigateway.security

import org.springframework.http.HttpMethod
import org.springframework.web.util.pattern.PathPattern

data class PublicPathRule(
    val method: HttpMethod,
    val pattern: PathPattern,
)
