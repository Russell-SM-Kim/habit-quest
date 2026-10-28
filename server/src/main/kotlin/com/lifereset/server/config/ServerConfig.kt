package com.lifereset.server.config

import java.time.ZoneId

/**
 * 서버 설정은 전부 환경 변수로 받는다 (코드에 비밀번호를 넣지 않기 위해).
 * Windows 에서는 scripts/windows/start-server.bat 에서 설정한다.
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    /** Google Cloud 의 "웹 애플리케이션" OAuth 클라이언트 ID. 앱 로그인 토큰의 대상(audience)이다. */
    val googleClientId: String,
    /** 로그인을 허용할 이메일. 비어 있으면 Google 계정이면 누구나 (Tailscale 안에서만 열려 있으니 보통은 본인만 접속). */
    val allowedEmails: Set<String>,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val zone: ZoneId,
) {
    fun isAllowed(email: String): Boolean = allowedEmails.isEmpty() || email.lowercase() in allowedEmails

    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig {
            val clientId = env["LIFE_RESET_GOOGLE_CLIENT_ID"].orEmpty().trim()
            require(clientId.endsWith(".apps.googleusercontent.com")) {
                "LIFE_RESET_GOOGLE_CLIENT_ID 에 Google 웹 클라이언트 ID(...apps.googleusercontent.com)를 설정하세요"
            }
            return ServerConfig(
                host = env["LIFE_RESET_HOST"] ?: "127.0.0.1",
                port = env["LIFE_RESET_PORT"]?.toIntOrNull() ?: 8080,
                googleClientId = clientId,
                allowedEmails = env["LIFE_RESET_ALLOWED_EMAILS"].orEmpty()
                    .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                dbUrl = env["LIFE_RESET_DB_URL"] ?: "jdbc:postgresql://localhost:5432/lifereset",
                dbUser = env["LIFE_RESET_DB_USER"] ?: "lifereset",
                dbPassword = env["LIFE_RESET_DB_PASSWORD"].orEmpty(),
                zone = ZoneId.of(env["LIFE_RESET_ZONE"] ?: "Asia/Seoul"),
            )
        }
    }
}
