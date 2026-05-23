package com.dbwp031.apigateway.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class UnauthorizedResponseWriter(
    private val objectMapper: ObjectMapper,
) {

    fun write(exchange: ServerWebExchange, errorCode: UnauthorizedErrorCode): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON

        val body = objectMapper.writeValueAsBytes(
            UnauthorizedResponse(
                code = errorCode.code,
                message = errorCode.message,
            ),
        )
        val buffer: DataBuffer = response.bufferFactory().wrap(body)
        return response.writeWith(Mono.just(buffer))
    }
}

data class UnauthorizedResponse(
    val code: String,
    val message: String,
)

enum class UnauthorizedErrorCode(
    val code: String,
    val message: String,
) {
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_TOKEN("INVALID_TOKEN", "유효하지 않은 토큰입니다."),
}
