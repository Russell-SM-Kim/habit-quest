package com.lifereset.core.protocol

import kotlinx.serialization.json.Json

/** 앱, 워치, 서버가 같은 설정으로 JSON 을 읽고 쓴다. 모르는 필드는 무시해서 버전이 달라도 깨지지 않는다. */
val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

/** 로그인 후 모든 요청에 붙이는 헤더: "Authorization: Bearer <세션 토큰>" */
const val AUTH_HEADER = "Authorization"
const val BEARER_PREFIX = "Bearer "
