package com.dbwp031.apigateway.filter

import com.dbwp031.apigateway.common.constants.GatewayHeaders
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant

@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val requestId = request.headers.getFirst(GatewayHeaders.REQUEST_ID) ?: "-"
        val startedAt = Instant.now()

        log.info(
            "Gateway request: requestId={}, method={}, path={}",
            requestId,
            request.method,
            request.uri.path,
        )

        return chain.filter(exchange)
            .doFinally {
                val elapsedMs = Duration.between(startedAt, Instant.now()).toMillis()
                log.info(
                    "Gateway response: requestId={}, status={}, elapsedMs={}",
                    requestId,
                    exchange.response.statusCode,
                    elapsedMs,
                )
            }
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1
}
