package com.dbwp031.apigateway.jwt

import com.dbwp031.apigateway.config.JwtProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class JwtTokenValidator(
    private val jwtProperties: JwtProperties,
    private val objectMapper: ObjectMapper,
) {

    fun validate(token: String): JwtClaims {
        val parts = token.split(".")
        if (parts.size != 3) {
            throw InvalidJwtTokenException()
        }

        val header = parseJson(parts[0])
        if (header.path("alg").asText() != HMAC_SHA256_ALGORITHM) {
            throw InvalidJwtTokenException()
        }

        val expectedSignature = sign("${parts[0]}.${parts[1]}")
        if (expectedSignature != parts[2]) {
            throw InvalidJwtTokenException()
        }

        val payload = parseJson(parts[1])
        if (payload.path("iss").asText() != jwtProperties.issuer) {
            throw InvalidJwtTokenException()
        }

        val expiresAt = payload.path("exp").asLong(0)
        if (expiresAt <= Instant.now().epochSecond) {
            throw ExpiredJwtTokenException()
        }

        val userId = payload.path("sub").asText("")
        val email = payload.path("email").asText("")
        val status = payload.path("status").asText("")
        val roles = payload.path("roles")
            .takeIf(JsonNode::isArray)
            ?.map { it.asText() }
            ?: emptyList()

        if (userId.isBlank() || email.isBlank() || status.isBlank()) {
            throw InvalidJwtTokenException()
        }

        return JwtClaims(
            userId = userId,
            email = email,
            roles = roles,
            status = status,
        )
    }

    private fun parseJson(base64Url: String): JsonNode =
        try {
            objectMapper.readTree(Base64.getUrlDecoder().decode(base64Url))
        } catch (_: Exception) {
            throw InvalidJwtTokenException()
        }

    private fun sign(content: String): String {
        val mac = Mac.getInstance(HMAC_SHA256_MAC)
        mac.init(SecretKeySpec(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256_MAC))
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)))
    }

    private companion object {
        const val HMAC_SHA256_ALGORITHM = "HS256"
        const val HMAC_SHA256_MAC = "HmacSHA256"
    }
}

class InvalidJwtTokenException : RuntimeException()

class ExpiredJwtTokenException : RuntimeException()
