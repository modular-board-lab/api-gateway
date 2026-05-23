package com.dbwp031.apigateway.security

import com.dbwp031.apigateway.config.SecurityPathProperties
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.pattern.PathPatternParser

@Component
class PublicPathMatcher(
    securityPathProperties: SecurityPathProperties,
) {

    private val parser = PathPatternParser.defaultInstance
    private val publicPaths = securityPathProperties.publicPaths.map {
        PublicPathRule(
            method = HttpMethod.valueOf(it.method.uppercase()),
            pattern = parser.parse(it.path),
        )
    }

    fun matches(exchange: ServerWebExchange): Boolean {
        val method = exchange.request.method
        val path = exchange.request.path.pathWithinApplication()

        return publicPaths.any { rule ->
            rule.method == method && rule.pattern.matches(path)
        }
    }
}
