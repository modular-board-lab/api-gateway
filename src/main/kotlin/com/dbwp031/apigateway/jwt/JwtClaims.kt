package com.dbwp031.apigateway.jwt

data class JwtClaims(
    val userId: String,
    val email: String,
    val roles: List<String>,
    val status: String,
)
