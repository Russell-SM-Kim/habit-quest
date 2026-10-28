package com.lifereset.server.auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Google 이 확인해 준 사용자. */
data class GoogleIdentity(
    /** Google 계정 고유 번호 (바뀌지 않음) */
    val subject: String,
    val email: String,
    val displayName: String,
)

/** 테스트에서 가짜로 바꿔 끼울 수 있게 인터페이스로 둔다. */
fun interface IdTokenVerifier {
    /** 올바른 토큰이면 사용자, 아니면 null */
    suspend fun verify(idToken: String): GoogleIdentity?
}

/**
 * Google 공식 라이브러리로 ID 토큰 검증:
 * 서명(Google 공개키), 만료 시각, 발급자(accounts.google.com), 대상(우리 클라이언트 ID), 이메일 인증 여부.
 * 공개키는 라이브러리가 받아서 캐시한다 (PC 가 인터넷에 연결돼 있어야 함).
 */
class GoogleApiIdTokenVerifier(clientId: String) : IdTokenVerifier {
    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(listOf(clientId))
        .build()

    override suspend fun verify(idToken: String): GoogleIdentity? = withContext(Dispatchers.IO) {
        val token = runCatching { verifier.verify(idToken) }.getOrNull() ?: return@withContext null
        val payload = token.payload
        if (payload.emailVerified != true || payload.email.isNullOrBlank()) return@withContext null
        GoogleIdentity(
            subject = payload.subject,
            email = payload.email.lowercase(),
            displayName = (payload["name"] as? String).orEmpty(),
        )
    }
}
