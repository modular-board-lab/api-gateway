package com.dbwp031.apigateway.jwt

import com.dbwp031.apigateway.config.JwtProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class JwtTokenValidatorTests {

    private val properties = JwtProperties(
        issuer = "auth-service",
        secret = "local-dev-secret-local-dev-secret-local-dev-secret",
    )
    private val objectMapper = ObjectMapper()
    private val validator = JwtTokenValidator(properties, objectMapper)

    @Test
    fun `valid token returns claims`() {
        val token = createToken(
            payload = mapOf(
                "iss" to "auth-service",
                "sub" to "1",
                "email" to "user@example.com",
                "roles" to listOf("USER"),
                "status" to "ACTIVE",
                "exp" to Instant.now().plusSeconds(60).epochSecond,
            ),
        )

        val claims = validator.validate(token)

        assertEquals("1", claims.userId)
        assertEquals("user@example.com", claims.email)
        assertEquals(listOf("USER"), claims.roles)
        assertEquals("ACTIVE", claims.status)
    }

    @Test
    fun `expired token throws expired exception`() {
        val token = createToken(
            payload = mapOf(
                "iss" to "auth-service",
                "sub" to "1",
                "email" to "user@example.com",
                "roles" to listOf("USER"),
                "status" to "ACTIVE",
                "exp" to Instant.now().minusSeconds(60).epochSecond,
            ),
        )

        assertThrows(ExpiredJwtTokenException::class.java) {
            validator.validate(token)
        }
    }

    @Test
    fun `invalid signature throws invalid token exception`() {
        val token = createToken(
            payload = mapOf(
                "iss" to "auth-service",
                "sub" to "1",
                "email" to "user@example.com",
                "roles" to listOf("USER"),
                "status" to "ACTIVE",
                "exp" to Instant.now().plusSeconds(60).epochSecond,
            ),
        )

        val tamperedToken = token.dropLast(1) + "x"

        assertThrows(InvalidJwtTokenException::class.java) {
            validator.validate(tamperedToken)
        }
    }

    private fun createToken(payload: Map<String, Any>): String {
        val header = mapOf(
            "alg" to "HS256",
            "typ" to "JWT",
        )
        val encodedHeader = encodeJson(header)
        val encodedPayload = encodeJson(payload)
        val signature = sign("$encodedHeader.$encodedPayload")

        return "$encodedHeader.$encodedPayload.$signature"
    }

    private fun encodeJson(value: Any): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(content: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mac.doFinal(content.toByteArray(StandardCharsets.UTF_8)))
    }
}
