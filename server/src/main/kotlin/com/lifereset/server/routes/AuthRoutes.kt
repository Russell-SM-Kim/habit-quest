package com.lifereset.server.routes

import com.lifereset.core.protocol.AUTH_HEADER
import com.lifereset.core.protocol.BEARER_PREFIX
import com.lifereset.core.protocol.auth.GoogleSignInRequest
import com.lifereset.server.auth.AuthException
import com.lifereset.server.auth.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

internal fun Route.authRoutes(auth: AuthService) {
    post("/auth/google") {
        val request = call.receive<GoogleSignInRequest>()
        require(request.idToken.isNotBlank()) { "idToken 이 비어 있습니다" }
        call.respond(auth.signInWithGoogle(request.idToken))
    }

    post("/auth/logout") {
        call.bearerToken()?.let(auth::signOut)
        call.respond(HttpStatusCode.NoContent)
    }
}

internal fun ApplicationCall.bearerToken(): String? =
    request.headers[AUTH_HEADER]
        ?.takeIf { it.startsWith(BEARER_PREFIX) }
        ?.removePrefix(BEARER_PREFIX)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** 로그인한 사용자의 id. 세션이 없거나 틀리면 401 (StatusPages 가 응답). */
internal fun ApplicationCall.requireUserId(auth: AuthService): String {
    val token = bearerToken() ?: throw AuthException(forbidden = false, message = "로그인이 필요합니다")
    return auth.userIdFor(token) ?: throw AuthException(forbidden = false, message = "로그인이 만료되었습니다")
}
