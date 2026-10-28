package com.lifereset.core.data.sync

import com.lifereset.core.data.repository.ServerConfig
import com.lifereset.core.protocol.API_KEY_HEADER
import com.lifereset.core.protocol.ProtocolJson
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

class ServerException(message: String) : IOException(message)

/** PC 서버 HTTP 클라이언트. 엔드포인트는 server 모듈의 Routing.kt 와 1:1 로 대응한다. */
class ServerApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun health(config: ServerConfig): HealthDto =
        execute(Request.Builder().url("${config.baseUrl}/health").get().build())
            .let { ProtocolJson.decodeFromString(it) }

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
        Request.Builder().url(config.baseUrl + path).header(API_KEY_HEADER, config.apiKey)

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            when {
                response.code == 401 -> throw ServerException("API 키가 서버와 다릅니다")
                !response.isSuccessful -> throw ServerException("서버 오류 ${response.code}")
                else -> text
            }
        }
    }
}
