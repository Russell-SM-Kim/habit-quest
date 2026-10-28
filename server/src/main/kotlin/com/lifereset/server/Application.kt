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

/** 서버 조립 지점: 설정 → DB → 로그인 → 동기화 저장소 → 경로. */
fun main() {
    val config = ServerConfig.fromEnv()
    val database = DatabaseFactory.connect(config)
    val auth = AuthService(database, config, GoogleApiIdTokenVerifier(config.googleClientId))
    val store = SyncStore(database)
    val engine = GameEngine()

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureRouting(config, auth, store, engine)
    }.start(wait = true)
}
