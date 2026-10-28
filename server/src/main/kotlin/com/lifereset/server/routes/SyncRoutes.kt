package com.lifereset.server.routes

import com.lifereset.core.game.engine.EngineInput
import com.lifereset.core.game.engine.GameEngine
import com.lifereset.core.protocol.sync.PushRequest
import com.lifereset.core.protocol.sync.toDomain
import com.lifereset.core.protocol.sync.toProfileDto
import com.lifereset.server.auth.AuthService
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.store.SyncStore
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.LocalDate

/** 동기화 경로. 전부 로그인 필요, 전부 로그인한 사용자의 기록만 다룬다. */
internal fun Route.syncRoutes(
    config: ServerConfig,
    auth: AuthService,
    store: SyncStore,
    engine: GameEngine,
) {
    post("/sync/push") {
        val userId = call.requireUserId(auth)
        call.respond(store.push(userId, call.receive<PushRequest>()))
    }

    get("/sync/pull") {
        val userId = call.requireUserId(auth)
        val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
        call.respond(store.pull(userId, since))
    }

    // 서버가 같은 엔진으로 계산한 프로필. 앱 화면과 숫자가 같아야 정상이다.
    get("/profile") {
        val userId = call.requireUserId(auth)
        val (quests, completions, settings) = store.snapshot(userId)
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
