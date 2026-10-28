package com.lifereset.core.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

/**
 * 폰의 Google 계정 선택 창을 띄워 ID 토큰을 받는다 (Android Credential Manager).
 *
 * @param activityContext 반드시 Activity (화면에서 LocalContext.current)
 * @param webClientId 서버의 /health 가 알려주는 Google "웹" 클라이언트 ID
 *
 * Google Cloud 에 "Android" 클라이언트(패키지 com.lifereset.app + 서명 SHA-1)도 등록돼 있어야
 * 계정 창이 뜬다. 안 하면 Failure("…developer console…") 가 난다. → docs/google-login-setup.md
 */
object GoogleSignIn {
    suspend fun requestIdToken(activityContext: Context, webClientId: String): GoogleSignInResult {
        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Failure("Google 계정 정보를 받지 못했습니다")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(e.message ?: e.type)
        }
    }
}
