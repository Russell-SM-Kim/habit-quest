package com.lifereset.server.routes

import com.lifereset.core.game.engine.GameEngine
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.sync.HealthDto
import com.lifereset.server.auth.AuthException
import com.lifereset.server.auth.AuthService
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.store.SyncStore
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

internal const val VERSION = "1.2.0"

/** 로그인 시도 제한: 같은 곳에서 1분에 20번까지. 여러 사람이 쓰는 서버에서 무작위 시도를 막는다. */
private val AUTH_LIMIT = RateLimitName("auth")

/**
 * 서버의 모든 경로:
 *   GET  /health          (공개) 연결 확인 + Google 클라이언트 ID
 *   POST /auth/google     (공개) Google ID 토큰 → 세션 토큰       → AuthRoutes.kt
 *   POST /auth/logout     (로그인)
 *   POST /sync/push       (로그인) 바뀐 기록 올리기                → SyncRoutes.kt
 *   GET  /sync/pull       (로그인) 바뀐 기록 받기
 *   GET  /profile         (로그인) 서버가 계산한 레벨·스탯 (검산용)
 */
fun Application.configureRouting(
    config: ServerConfig,
    auth: AuthService,
    store: SyncStore,
    engine: GameEngine,
) {
    install(ContentNegotiation) { json(ProtocolJson) }
    install(CallLogging)
    install(RateLimit) {
        register(AUTH_LIMIT) {
            rateLimiter(limit = 20, refillPeriod = 1.minutes)
            // Tailscale serve/funnel 을 거치면 원래 주소는 X-Forwarded-For 에 들어 있다
            requestKey { call ->
                call.request.headers["X-Forwarded-For"]?.substringBefore(',')?.trim()
                    ?: call.request.origin.remoteHost
            }
        }
    }
    install(StatusPages) {
        exception<AuthException> { call, cause ->
            val status = if (cause.forbidden) HttpStatusCode.Forbidden else HttpStatusCode.Unauthorized
            call.respond(status, ErrorBody(cause.message ?: "unauthorized"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorBody(cause.message ?: "bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("요청 처리 실패", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorBody("server error"))
        }
    }

    routing {
        get("/health") {
            call.respond(
                HealthDto(
                    ok = true,
                    serverTime = System.currentTimeMillis(),
                    version = VERSION,
                    googleClientId = config.googleClientId,
                ),
            )
        }
        rateLimit(AUTH_LIMIT) {
            authRoutes(auth)
        }
        syncRoutes(config, auth, store, engine)
    }
}

@Serializable
internal data class ErrorBody(val error: String)
