package com.dbwp031.apigateway.common.constants

object GatewayHeaders {
    const val REQUEST_ID = "X-Request-Id"
    const val USER_ID = "X-User-Id"
    const val USER_EMAIL = "X-User-Email"
    const val USER_ROLES = "X-User-Roles"
    const val ACCOUNT_STATUS = "X-Account-Status"

    val USER_CONTEXT_HEADERS = listOf(
        USER_ID,
        USER_EMAIL,
        USER_ROLES,
        ACCOUNT_STATUS,
    )
}
