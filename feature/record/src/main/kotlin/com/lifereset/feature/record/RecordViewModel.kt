package com.lifereset.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.game.model.DaySummary
import com.lifereset.core.game.model.PlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 그리드 한 칸. null = 아직 오지 않은 날. */
data class GridCell(val dayNumber: Int, val summary: DaySummary?, val isToday: Boolean)

data class RecordUiState(
    val player: PlayerState? = null,
    val cells: List<GridCell> = emptyList(),
)

class RecordViewModel(game: GameRepository) : ViewModel() {

    val uiState: StateFlow<RecordUiState> = game.state.map { player ->
        if (player == null) return@map RecordUiState()
        val season = player.season
        val byDate = player.history.associateBy { it.date }
        val cells = (0 until season.seasonLength).map { index ->
            val date = season.seasonStart.plusDays(index.toLong())
            GridCell(
                dayNumber = index + 1,
                summary = byDate[date],
                isToday = date == player.today.date,
            )
        }
        RecordUiState(player, cells)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordUiState())

    companion object {
        fun factory(game: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { RecordViewModel(game) } }
    }
}
