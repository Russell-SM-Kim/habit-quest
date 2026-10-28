package com.lifereset.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestSlot
import com.lifereset.core.game.rules.RecoveryQuest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val player: PlayerState? = null,
    val playerName: String = "",
)

/** 오늘 화면에서 열리는 작은 창. */
sealed interface TodayDialog {
    /** 완료 전: 택1 고르기, 실제로 한 양, 승패 입력 */
    data class Complete(val slot: QuestSlot) : TodayDialog

    /** 완료 후: 기록 추가 또는 완료 취소 */
    data class Completed(val slot: QuestSlot) : TodayDialog
}

class TodayViewModel(
    private val game: GameRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(game.state, settings.playerName) { player, name ->
        TodayUiState(player, name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private val _dialog = MutableStateFlow<TodayDialog?>(null)
    val dialog: StateFlow<TodayDialog?> = _dialog.asStateFlow()

    fun onSlotTap(slot: QuestSlot) {
        _dialog.value = when {
            slot.done -> TodayDialog.Completed(slot)
            slot.needsInput() -> TodayDialog.Complete(slot)
            else -> {
                complete(slot.primary)
                null
            }
        }
    }

    /** 길게 누르면 입력 없는 퀘스트도 창을 연다 (양을 기록하고 싶을 때). */
    fun onSlotLongPress(slot: QuestSlot) {
        _dialog.value = if (slot.done) TodayDialog.Completed(slot) else TodayDialog.Complete(slot)
    }

    fun complete(quest: QuestDef, amount: Double? = null, won: Boolean = false) {
        _dialog.value = null
        viewModelScope.launch { game.complete(quest.id, amount, won) }
    }

    fun completeRecovery() {
        viewModelScope.launch { game.complete(RecoveryQuest.ID) }
    }

    fun undo(completionId: String) {
        _dialog.value = null
        viewModelScope.launch { game.undo(completionId) }
    }

    fun addAmount(completionId: String, extra: Double) {
        _dialog.value = null
        viewModelScope.launch { game.addAmount(completionId, extra) }
    }

    fun dismissDialog() {
        _dialog.value = null
    }

    private fun QuestSlot.needsInput(): Boolean =
        isChoice || quests.any { it.target != null || it.tracksWin }

    companion object {
        fun factory(game: GameRepository, settings: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { TodayViewModel(game, settings) } }
    }
}
