package com.lifereset.server

import com.lifereset.core.game.engine.GameEngine
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.db.DatabaseFactory
import com.lifereset.server.routes.configureRouting
import com.lifereset.server.store.SyncStore
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = ServerConfig.fromEnv()
    val database = DatabaseFactory.connect(config)
    val store = SyncStore(database)
    val engine = GameEngine()

    embeddedServer(Netty, port = config.port, host = config.host) {
        configureRouting(config, store, engine)
    }.start(wait = true)
}
