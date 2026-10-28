package com.lifereset.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.QuestSlot
import com.lifereset.feature.today.component.BossPanel
import com.lifereset.feature.today.component.DailyQuestPanel
import com.lifereset.feature.today.component.PlayerHeader
import com.lifereset.feature.today.component.RecoveryPanel

/** 앱에서 이 화면을 붙일 때 쓰는 진입점. 의존성은 바깥에서 받는다. */
@Composable
fun TodayRoute(
    game: GameRepository,
    settings: SettingsRepository,
    onAddQuest: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(game, settings)),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()

    TodayScreen(
        ui = ui,
        onSlotTap = viewModel::onSlotTap,
        onSlotLongPress = viewModel::onSlotLongPress,
        onRecovery = viewModel::completeRecovery,
        onAddQuest = onAddQuest,
    )

    when (val d = dialog) {
        is TodayDialog.Complete -> CompleteDialog(
            slot = d.slot,
            onConfirm = viewModel::complete,
            onDismiss = viewModel::dismissDialog,
        )
        is TodayDialog.Completed -> CompletedDialog(
            slot = d.slot,
            onAddAmount = viewModel::addAmount,
            onUndo = viewModel::undo,
            onDismiss = viewModel::dismissDialog,
        )
        null -> Unit
    }
}

@Composable
fun TodayScreen(
    ui: TodayUiState,
    onSlotTap: (QuestSlot) -> Unit,
    onSlotLongPress: (QuestSlot) -> Unit,
    onRecovery: () -> Unit,
    onAddQuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player: PlayerState = ui.player ?: run {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LrTheme.colors.onSky)
        }
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = LrTheme.Dimens.screenPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerHeader(player = player, name = ui.playerName)
        if (player.today.recoveryAvailable) {
            RecoveryPanel(onComplete = onRecovery)
        }
        DailyQuestPanel(
            player = player,
            onSlotTap = onSlotTap,
            onSlotLongPress = onSlotLongPress,
        )
        player.boss?.let { BossPanel(it) }
        LrWideButton(text = "퀘스트 추가", onClick = onAddQuest, icon = LrIcons.Plus)
    }
}
