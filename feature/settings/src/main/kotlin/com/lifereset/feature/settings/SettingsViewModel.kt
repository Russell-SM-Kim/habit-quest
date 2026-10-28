package com.lifereset.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import com.lifereset.core.data.auth.GoogleSignIn
import com.lifereset.core.data.auth.GoogleSignInResult
import com.lifereset.core.data.repository.Account
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.data.repository.SyncStatus
import com.lifereset.core.data.sync.SyncManager
import com.lifereset.core.data.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SettingsForm(
    val name: String = "",
    val serverUrl: String = "",
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val sync: SyncManager,
) : ViewModel() {

    private val _form = MutableStateFlow(SettingsForm())
    val form: StateFlow<SettingsForm> = _form.asStateFlow()

    /** 버튼을 누른 결과 한 줄 (연결 확인, 동기화, 복원). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> =
        settings.syncStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus.Never)

    val gameStart: StateFlow<LocalDate?> =
        settings.gameStart.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 로그인한 Google 계정. null = 로그아웃 상태 */
    val account: StateFlow<Account?> =
        settings.account.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val backgroundUri: StateFlow<String?> =
        settings.backgroundUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val soundEnabled: StateFlow<Boolean> =
        settings.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            _form.value = SettingsForm(
                name = settings.playerName.first(),
                serverUrl = settings.serverUrl(),
            )
        }
    }

    fun edit(transform: (SettingsForm) -> SettingsForm) = _form.update(transform)

    fun saveName() = viewModelScope.launch {
        settings.setPlayerName(_form.value.name)
        _message.value = "이름을 저장했습니다"
    }

    fun setBackground(uri: String?) = viewModelScope.launch {
        settings.setBackgroundUri(uri)
        _message.value = if (uri == null) "기본 배경으로 되돌렸습니다" else "배경을 바꿨습니다"
    }

    fun setSound(enabled: Boolean) = viewModelScope.launch { settings.setSoundEnabled(enabled) }

    /** 주소 저장 + 연결 확인. */
    fun saveServer() = runBusy {
        val url = _form.value.serverUrl.trim().trimEnd('/')
        if (!url.startsWith("http")) return@runBusy "주소는 https:// 로 시작해야 합니다"
        settings.setServerUrl(url)
        sync.checkServer(url).fold({ "연결됨 (서버 v${it.version}) · 이제 Google 로 로그인하세요" }, { "연결 실패: ${it.message}" })
    }

    /**
     * Google 로그인. activityContext 는 화면의 LocalContext (계정 선택 창을 띄울 Activity).
     * 1) 서버에서 클라이언트 ID 받기 → 2) 계정 선택 → 3) 서버 로그인 + 기록 합치기
     */
    fun signIn(activityContext: Context) = runBusy {
        val url = _form.value.serverUrl.trim().trimEnd('/')
        if (!url.startsWith("http")) return@runBusy "서버 주소를 먼저 입력하세요"
        val server = sync.checkServer(url).getOrElse { return@runBusy "서버 연결 실패: ${it.message}" }
        when (val google = GoogleSignIn.requestIdToken(activityContext, server.googleClientId)) {
            GoogleSignInResult.Cancelled -> "로그인을 취소했습니다"
            is GoogleSignInResult.Failure -> "Google 로그인 실패: ${google.message}"
            is GoogleSignInResult.Success -> sync.signIn(url, google.idToken).fold(
                { "${it.email} 로 로그인했습니다. 기록을 서버와 합쳤습니다" },
                { "서버 로그인 실패: ${it.message}" },
            )
        }
    }

    fun signOut() = runBusy {
        sync.signOut()
        "로그아웃했습니다. 이 폰의 기록은 그대로 남아 있습니다"
    }

    fun syncNow() = runBusy { describe(sync.sync()) }

    fun restore() = runBusy { describe(sync.restoreFromServer()) }

    private fun describe(result: SyncResult): String = when (result) {
        is SyncResult.Success -> "완료 · 올림 ${result.pushed}건, 받음 ${result.pulled}건"
        SyncResult.NotConfigured -> "서버 주소를 저장하고 Google 로 로그인하세요"
        is SyncResult.Failure -> "실패: ${result.message}"
    }

    private fun runBusy(block: suspend () -> String) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _message.value = runCatching { block() }.getOrElse { "오류: ${it.message}" }
            _busy.value = false
        }
    }

    companion object {
        fun factory(settings: SettingsRepository, sync: SyncManager): ViewModelProvider.Factory =
            viewModelFactory { initializer { SettingsViewModel(settings, sync) } }
    }
}
