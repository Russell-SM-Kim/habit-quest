package com.lifereset.server.config

import java.time.ZoneId

/**
 * 서버 설정은 전부 환경 변수로 받는다 (코드에 비밀번호를 넣지 않기 위해).
 * Windows 에서는 scripts/windows/start-server.bat 에서 설정한다.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val apiKey: String,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val zone: ZoneId,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig {
            val apiKey = env["LIFE_RESET_API_KEY"].orEmpty()
            require(apiKey.length >= 16) {
                "LIFE_RESET_API_KEY 환경 변수에 16자 이상의 키를 설정하세요"
            }
            return ServerConfig(
                host = env["LIFE_RESET_HOST"] ?: "0.0.0.0",
                port = env["LIFE_RESET_PORT"]?.toIntOrNull() ?: 8080,
                apiKey = apiKey,
                dbUrl = env["LIFE_RESET_DB_URL"] ?: "jdbc:postgresql://localhost:5432/lifereset",
                dbUser = env["LIFE_RESET_DB_USER"] ?: "lifereset",
                dbPassword = env["LIFE_RESET_DB_PASSWORD"].orEmpty(),
                zone = ZoneId.of(env["LIFE_RESET_ZONE"] ?: "Asia/Seoul"),
            )
        }
    }
}
