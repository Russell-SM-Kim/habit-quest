package com.lifereset.core.data.sync

import com.lifereset.core.data.repository.ServerConfig
import com.lifereset.core.protocol.AUTH_HEADER
import com.lifereset.core.protocol.BEARER_PREFIX
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.auth.AuthResponse
import com.lifereset.core.protocol.auth.GoogleSignInRequest
import com.lifereset.core.protocol.sync.HealthDto
import com.lifereset.core.protocol.sync.PullResponse
import com.lifereset.core.protocol.sync.PushRequest
import com.lifereset.core.protocol.sync.PushResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

open class ServerException(message: String) : IOException(message)

/** 세션이 없거나 만료됨 → 다시 로그인해야 한다. */
class UnauthorizedException(message: String) : ServerException(message)

/** PC 서버 HTTP 클라이언트. 엔드포인트는 server 모듈의 routes/ 와 1:1 로 대응한다. */
class ServerApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun health(baseUrl: String): HealthDto =
        ProtocolJson.decodeFromString(execute(Request.Builder().url("$baseUrl/health").get().build()))

    suspend fun signInWithGoogle(baseUrl: String, idToken: String): AuthResponse {
        val body = ProtocolJson.encodeToString(GoogleSignInRequest(idToken)).toRequestBody(jsonType)
        return ProtocolJson.decodeFromString(execute(Request.Builder().url("$baseUrl/auth/google").post(body).build()))
    }

    suspend fun signOut(config: ServerConfig) {
        execute(authorized(config, "/auth/logout").post(ByteArray(0).toRequestBody(null)).build())
    }

    suspend fun push(config: ServerConfig, request: PushRequest): PushResponse {
        val body = ProtocolJson.encodeToString(request).toRequestBody(jsonType)
        val http = authorized(config, "/sync/push").post(body).build()
        return ProtocolJson.decodeFromString(execute(http))
    }

    suspend fun pull(config: ServerConfig, since: Long): PullResponse {
        val http = authorized(config, "/sync/pull?since=$since").get().build()
        return ProtocolJson.decodeFromString(execute(http))
    }

    private fun authorized(config: ServerConfig, path: String): Request.Builder =
        Request.Builder().url(config.baseUrl + path).header(AUTH_HEADER, BEARER_PREFIX + config.sessionToken)

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            when {
                response.code == 401 -> throw UnauthorizedException("로그인이 만료되었습니다. 다시 로그인하세요")
                response.code == 403 -> throw ServerException(serverMessage(text) ?: "허용되지 않은 계정입니다")
                !response.isSuccessful -> throw ServerException("서버 오류 ${response.code}")
                else -> text
            }
        }
    }

    /** 서버의 {"error": "..."} 에서 메시지만 꺼낸다. */
    private fun serverMessage(text: String): String? =
        Regex("\"error\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1)
}
