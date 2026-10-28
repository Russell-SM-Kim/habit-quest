package com.lifereset.core.protocol.auth

import kotlinx.serialization.Serializable

/*
 * 로그인 흐름
 *  1) 앱: Google 계정 선택 → Google 이 서명한 ID 토큰을 받는다 (약 1시간 유효)
 *  2) 앱 → 서버  POST /auth/google  { idToken }
 *  3) 서버: Google 서명·대상(클라이언트 ID)·이메일 확인 → 사용자 생성/조회 → 세션 토큰 발급
 *  4) 앱: 세션 토큰을 저장하고 이후 모든 동기화 요청에 "Authorization: Bearer 토큰"
 *  로그아웃하면 서버에서 세션을 지운다.
 */

@Serializable
data class GoogleSignInRequest(val idToken: String)

@Serializable
data class AuthResponse(
    val sessionToken: String,
    val email: String,
    val displayName: String,
)
