package com.lifereset.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifereset.core.protocol.wear.WearSnapshot
import com.lifereset.wear.data.PhoneLink
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WearUiState(
    val snapshot: WearSnapshot? = null,
    /** 폰에 보냈지만 아직 결과가 안 돌아온 퀘스트 (바로 체크된 것처럼 보여준다) */
    val pending: Set<String> = emptySet(),
    val phoneMissing: Boolean = false,
)

/** 워치 화면에서 생기는 사건 (진동·연출용). */
sealed interface WearEvent {
    data class LevelUp(val level: Int) : WearEvent
    data object Cleared : WearEvent
}

class WearViewModel(app: Application) : AndroidViewModel(app) {
    private val link = PhoneLink(app)
    private val pending = MutableStateFlow<Set<String>>(emptySet())
    private val phoneMissing = MutableStateFlow(false)
    private val _events = MutableSharedFlow<WearEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<WearEvent> = _events.asSharedFlow()

    val ui: StateFlow<WearUiState> = combine(link.snapshot, pending, phoneMissing) { snapshot, waiting, missing ->
        // 폰이 완료를 반영해 돌려주면 대기 표시를 지운다
        val done = snapshot?.quests?.filter { it.done }?.map { it.questId }.orEmpty().toSet()
        WearUiState(snapshot, waiting - done, missing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearUiState())

    private var lastLevel: Int? = null

    init {
        link.start()
        viewModelScope.launch {
            runCatching { link.loadLatest() }
            phoneMissing.value = !runCatching { link.requestRefresh() }.getOrDefault(false)
        }
        viewModelScope.launch {
            link.snapshot.collect { snapshot ->
                if (snapshot == null) return@collect
                val before = lastLevel
                if (before != null && snapshot.level > before) _events.emit(WearEvent.LevelUp(snapshot.level))
                lastLevel = snapshot.level
                pending.value = pending.value - snapshot.quests.filter { it.done }.map { it.questId }.toSet()
            }
        }
    }

    fun complete(questId: String) {
        if (questId in pending.value) return
        pending.value = pending.value + questId
        viewModelScope.launch {
            val sent = runCatching { link.sendComplete(questId) }.getOrDefault(false)
            if (sent) {
                _events.emit(WearEvent.Cleared)
            } else {
                pending.value = pending.value - questId
                phoneMissing.value = true
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            phoneMissing.value = !runCatching { link.requestRefresh() }.getOrDefault(false)
        }
    }

    override fun onCleared() {
        link.stop()
    }
}
