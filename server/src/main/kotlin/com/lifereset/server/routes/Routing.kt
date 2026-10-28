package com.lifereset.server.routes

import com.lifereset.core.game.engine.EngineInput
import com.lifereset.core.game.engine.GameEngine
import com.lifereset.core.protocol.API_KEY_HEADER
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.sync.HealthDto
import com.lifereset.core.protocol.sync.PushRequest
import com.lifereset.core.protocol.sync.toDomain
import com.lifereset.core.protocol.sync.toProfileDto
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.store.SyncStore
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.security.MessageDigest
import java.time.LocalDate

private const val VERSION = "1.0.0"

fun Application.configureRouting(config: ServerConfig, store: SyncStore, engine: GameEngine) {
    install(ContentNegotiation) { json(ProtocolJson) }
    install(CallLogging)
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorBody(cause.message ?: "bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("요청 처리 실패", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorBody("server error"))
        }
    }

    val keyGuard = ApiKeyGuard(config.apiKey)

    routing {
        // 키 없이 열려 있는 유일한 경로: 연결 확인용
        get("/health") {
            call.respond(HealthDto(ok = true, serverTime = System.currentTimeMillis(), version = VERSION))
        }

        post("/sync/push") {
            if (keyGuard.reject(call)) return@post
            call.respond(store.push(call.receive<PushRequest>()))
        }

        get("/sync/pull") {
            if (keyGuard.reject(call)) return@get
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            call.respond(store.pull(since))
        }

        // 서버가 같은 엔진으로 계산한 프로필. 앱 화면과 숫자가 같아야 정상이다.
        get("/profile") {
            if (keyGuard.reject(call)) return@get
            val (quests, completions, settings) = store.snapshot()
            val today = LocalDate.now(config.zone)
            val start = settings?.gameStart?.let(LocalDate::parse) ?: today
            val state = engine.compute(
                EngineInput(
                    gameStart = start,
                    today = today,
                    quests = quests.map { it.toDomain() },
                    completions = completions.map { it.toDomain() },
                ),
            )
            call.respond(state.toProfileDto())
        }
    }
}

@kotlinx.serialization.Serializable
private data class ErrorBody(val error: String)

/** X-Api-Key 헤더 검사. 틀리면 401 로 응답하고 true 를 돌려준다. */
private class ApiKeyGuard(apiKey: String) {
    private val expected = apiKey.toByteArray()

    suspend fun reject(call: ApplicationCall): Boolean {
        val given = call.request.headers[API_KEY_HEADER]?.toByteArray() ?: ByteArray(0)
        // 응답 시간 차로 키를 추측하지 못하게 상수 시간 비교
        if (MessageDigest.isEqual(given, expected)) return false
        call.respond(HttpStatusCode.Unauthorized, ErrorBody("invalid api key"))
        return true
    }
}
