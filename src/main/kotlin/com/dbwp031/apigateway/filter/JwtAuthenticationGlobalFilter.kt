package com.dbwp031.apigateway.filter

import com.dbwp031.apigateway.common.constants.GatewayHeaders
import com.dbwp031.apigateway.jwt.ExpiredJwtTokenException
import com.dbwp031.apigateway.jwt.InvalidJwtTokenException
import com.dbwp031.apigateway.jwt.JwtClaims
import com.dbwp031.apigateway.jwt.JwtTokenValidator
import com.dbwp031.apigateway.security.PublicPathMatcher
import com.dbwp031.apigateway.security.UnauthorizedErrorCode
import com.dbwp031.apigateway.security.UnauthorizedResponseWriter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationGlobalFilter(
    private val publicPathMatcher: PublicPathMatcher,
    private val jwtTokenValidator: JwtTokenValidator,
    private val unauthorizedResponseWriter: UnauthorizedResponseWriter,
) : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val sanitizedExchange = removeUntrustedUserContextHeaders(exchange)

        if (publicPathMatcher.matches(sanitizedExchange)) {
            return chain.filter(sanitizedExchange)
        }

        val token = extractBearerToken(sanitizedExchange)
            ?: return unauthorizedResponseWriter.write(sanitizedExchange, UnauthorizedErrorCode.UNAUTHORIZED)

        val claims = try {
            jwtTokenValidator.validate(token)
        } catch (_: ExpiredJwtTokenException) {
            return unauthorizedResponseWriter.write(sanitizedExchange, UnauthorizedErrorCode.TOKEN_EXPIRED)
        } catch (_: InvalidJwtTokenException) {
            return unauthorizedResponseWriter.write(sanitizedExchange, UnauthorizedErrorCode.INVALID_TOKEN)
        }

        return chain.filter(addUserContextHeaders(sanitizedExchange, claims))
    }

    private fun extractBearerToken(exchange: ServerWebExchange): String? {
        val authorization = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            ?: return null

        if (!authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            return null
        }

        return authorization.substring(BEARER_PREFIX.length).trim().takeIf(String::isNotBlank)
    }

    private fun removeUntrustedUserContextHeaders(exchange: ServerWebExchange): ServerWebExchange =
        exchange.mutate()
            .request { request ->
                request.headers { headers ->
                    GatewayHeaders.USER_CONTEXT_HEADERS.forEach(headers::remove)
                }
            }
            .build()

    private fun addUserContextHeaders(exchange: ServerWebExchange, claims: JwtClaims): ServerWebExchange =
        exchange.mutate()
            .request { request ->
                request.headers { headers ->
                    headers.set(GatewayHeaders.USER_ID, claims.userId)
                    headers.set(GatewayHeaders.USER_EMAIL, claims.email)
                    headers.set(GatewayHeaders.USER_ROLES, claims.roles.joinToString(","))
                    headers.set(GatewayHeaders.ACCOUNT_STATUS, claims.status)
                }
            }
            .build()

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 2

    private companion object {
        const val BEARER_PREFIX = "Bearer "
    }
}
