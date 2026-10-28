package com.lifereset.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.auth.AccountManager
import com.lifereset.core.data.auth.SignInOutcome
import com.lifereset.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WelcomeUiState(
    /** 서버 주소. 앱에 기본 주소가 들어 있으면 미리 채워져 있다. */
    val serverUrl: String = "",
    /** 기본 주소가 없을 때만 주소 입력칸을 보여준다. */
    val showServerField: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)

class WelcomeViewModel(
    private val settings: SettingsRepository,
    private val accounts: AccountManager,
) : ViewModel() {

    private val _ui = MutableStateFlow(WelcomeUiState())
    val ui: StateFlow<WelcomeUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val url = settings.serverUrl()
            _ui.update { it.copy(serverUrl = url, showServerField = url.isBlank()) }
        }
    }

    fun editServerUrl(url: String) = _ui.update { it.copy(serverUrl = url, message = null) }

    /** 시작 화면이 사라지는 건 저장소의 onboardingDone 이 바뀌면서 앱이 알아서 처리한다. */
    fun signIn(activityContext: Context) {
        if (_ui.value.busy) return
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            val url = _ui.value.serverUrl.trim().trimEnd('/')
            val message = when (val result = accounts.signInWithGoogle(activityContext, url)) {
                is SignInOutcome.Success -> null
                SignInOutcome.Cancelled -> "로그인을 취소했습니다"
                is SignInOutcome.Failure -> result.message
            }
            _ui.update { it.copy(busy = false, message = message, showServerField = it.showServerField || message != null && url.isBlank()) }
        }
    }

    fun continueWithoutAccount() {
        viewModelScope.launch { accounts.continueWithoutAccount() }
    }

    companion object {
        fun factory(settings: SettingsRepository, accounts: AccountManager): ViewModelProvider.Factory =
            viewModelFactory { initializer { WelcomeViewModel(settings, accounts) } }
    }
}
