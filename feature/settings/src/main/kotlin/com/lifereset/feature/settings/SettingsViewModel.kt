package com.lifereset.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.repository.ServerConfig
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
    val apiKey: String = "",
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

    val backgroundUri: StateFlow<String?> =
        settings.backgroundUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val soundEnabled: StateFlow<Boolean> =
        settings.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    init {
        viewModelScope.launch {
            val server = settings.serverConfig()
            _form.value = SettingsForm(
                name = settings.playerName.first(),
                serverUrl = server?.baseUrl.orEmpty(),
                apiKey = server?.apiKey.orEmpty(),
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

    fun saveServer() = runBusy {
        val config = ServerConfig(_form.value.serverUrl.trim(), _form.value.apiKey.trim())
        if (!config.isValid) return@runBusy "주소는 http(s):// 로 시작하고, 키는 16자 이상이어야 합니다"
        settings.setServerConfig(config)
        sync.testConnection().fold({ it }, { "연결 실패: ${it.message}" })
    }

    fun syncNow() = runBusy { describe(sync.sync()) }

    fun restore() = runBusy { describe(sync.restoreFromServer()) }

    private fun describe(result: SyncResult): String = when (result) {
        is SyncResult.Success -> "완료 · 올림 ${result.pushed}건, 받음 ${result.pulled}건"
        SyncResult.NotConfigured -> "서버 주소와 키를 먼저 저장하세요"
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
