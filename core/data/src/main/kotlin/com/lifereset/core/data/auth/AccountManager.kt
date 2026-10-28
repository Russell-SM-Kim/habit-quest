package com.lifereset.core.data.auth

import android.content.Context
import com.lifereset.core.data.repository.Account
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.data.sync.SyncManager

/** 로그인 결과. 화면은 이것만 보고 메시지를 정한다. */
sealed interface SignInOutcome {
    data class Success(val account: Account) : SignInOutcome
    data object Cancelled : SignInOutcome
    data class Failure(val message: String) : SignInOutcome
}

/**
 * 로그인·로그아웃을 한 곳에 모은 것. 시작 화면과 설정 화면이 같이 쓴다.
 *
 * Google 로그인 순서
 *  1) 서버 /health 에서 Google 클라이언트 ID 받기 (앱에 ID 를 따로 넣지 않기 위해)
 *  2) 폰의 Google 계정 선택 창 → ID 토큰
 *  3) 서버 로그인 → 세션 토큰 저장 → 서버 기록과 합치기 → 동기화
 */
class AccountManager(
    private val settings: SettingsRepository,
    private val sync: SyncManager,
) {
    /**
     * @param activityContext 계정 선택 창을 띄울 Activity (화면의 LocalContext)
     * @param serverUrl 이번에 쓸 서버 주소. null 이면 저장된 주소(없으면 앱에 내장된 기본 주소)
     */
    suspend fun signInWithGoogle(activityContext: Context, serverUrl: String? = null): SignInOutcome {
        val url = (serverUrl ?: settings.serverUrl()).trim().trimEnd('/')
        if (!url.startsWith("http")) return SignInOutcome.Failure("서버 주소가 없습니다. https:// 로 시작하는 주소를 입력하세요")

        val server = sync.checkServer(url).getOrElse {
            return SignInOutcome.Failure("서버에 연결할 수 없습니다: ${it.message}")
        }
        val idToken = when (val google = GoogleSignIn.requestIdToken(activityContext, server.googleClientId)) {
            GoogleSignInResult.Cancelled -> return SignInOutcome.Cancelled
            is GoogleSignInResult.Failure -> return SignInOutcome.Failure("Google 로그인 실패: ${google.message}")
            is GoogleSignInResult.Success -> google.idToken
        }
        return sync.signIn(url, idToken).fold(
            onSuccess = { account ->
                settings.setOnboardingDone()
                SignInOutcome.Success(account)
            },
            onFailure = { SignInOutcome.Failure("서버 로그인 실패: ${it.message}") },
        )
    }

    /** 로그인하지 않고 쓰기: 기록은 이 폰에만 저장된다. 나중에 설정에서 로그인하면 합쳐진다. */
    suspend fun continueWithoutAccount() = settings.setOnboardingDone()

    suspend fun signOut() = sync.signOut()
}
