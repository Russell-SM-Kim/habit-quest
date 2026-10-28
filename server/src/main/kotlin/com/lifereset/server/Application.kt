package com.lifereset.server

import com.lifereset.core.game.engine.GameEngine
import com.lifereset.server.auth.AuthService
import com.lifereset.server.auth.GoogleApiIdTokenVerifier
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.db.DatabaseFactory
import com.lifereset.server.routes.configureRouting
import com.lifereset.server.store.SyncStore
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

/** 서버 조립 지점: 설정 → DB → 로그인 → 동기화 저장소 → 경로. */
fun main() {
    val config = ServerConfig.fromEnv()
    val log = LoggerFactory.getLogger("HabitQuest")
    if (config.allowedEmails.isEmpty()) {
        log.warn("LIFE_RESET_ALLOWED_EMAILS 가 비어 있습니다 → Google 계정이면 누구나 로그인할 수 있습니다 (공개 가입)")
    } else {
        log.info("로그인 허용 계정 ${config.allowedEmails.size}개")
    }
    val database = DatabaseFactory.connect(config)
    val auth = AuthService(database, config, GoogleApiIdTokenVerifier(config.googleClientId))
    val store = SyncStore(database)
    val engine = GameEngine()

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureRouting(config, auth, store, engine)
    }.start(wait = true)
}
