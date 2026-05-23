package com.dbwp031.apigateway.filter

import com.dbwp031.apigateway.common.constants.GatewayHeaders
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class RequestIdGlobalFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val requestId = exchange.request.headers.getFirst(GatewayHeaders.REQUEST_ID)
            ?: UUID.randomUUID().toString()

        val mutatedExchange = exchange.mutate()
            .request { request ->
                request.headers { headers ->
                    headers.set(GatewayHeaders.REQUEST_ID, requestId)
                }
            }
            .build()

        mutatedExchange.response.beforeCommit {
            mutatedExchange.response.headers.set(GatewayHeaders.REQUEST_ID, requestId)
            Mono.empty()
        }

        return chain.filter(mutatedExchange)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
