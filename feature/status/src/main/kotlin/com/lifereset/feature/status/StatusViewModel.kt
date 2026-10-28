package com.lifereset.feature.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.game.model.PlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatusUiState(val player: PlayerState? = null, val name: String = "")

class StatusViewModel(game: GameRepository, settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<StatusUiState> = combine(game.state, settings.playerName) { player, name -> StatusUiState(player, name) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())

    companion object {
        fun factory(game: GameRepository, settings: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { StatusViewModel(game, settings) } }
    }
}
