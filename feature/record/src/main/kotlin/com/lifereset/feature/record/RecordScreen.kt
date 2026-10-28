package com.lifereset.feature.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.LrCard
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.PlayerState

@Composable
fun RecordRoute(
    game: GameRepository,
    viewModel: RecordViewModel = viewModel(factory = RecordViewModel.factory(game)),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val player = ui.player ?: return
    RecordScreen(player, ui.cells)
}

@Composable
fun RecordScreen(player: PlayerState, cells: List<GridCell>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = LrTheme.Dimens.screenPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeasonPanel(player)
        HabitGrid(cells)
        Milestones(player.season.day)
        Totals(player)
        PastSeasons(player)
    }
}

/** 하늘 위 큰 글씨: Season 1 · Day 23 / 66 */
@Composable
private fun SeasonPanel(player: PlayerState) {
    val season = player.season
    HeroHeader(
        title = "Season ${season.season}",
        suffix = "Day ${season.day} / ${season.seasonLength}",
        caption = "${season.daysLeft}일 남음 · 끝나면 시즌 ${season.season + 1}이 자동 시작 · 누적 ${season.totalDay}일차",
        progress = season.progress,
    )
}

@Composable
private fun HabitGrid(cells: List<GridCell>) {
    val colors = LrTheme.colors
    val done = cells.count { it.summary?.success == true && it.summary.isRestDay.not() }
    LrPanel(contentPadding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("달성 기록", style = LrTheme.type.bodyStrong, color = colors.textPrimary)
            Text("기준 달성 ${done}일", style = LrTheme.type.caption, color = colors.textSecondary)
        }
        Column(
            Modifier.semantics { contentDescription = "시즌 ${cells.size}일 중 ${done}일 달성" },
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            cells.chunked(11).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { cell -> GridSquare(cell, Modifier.weight(1f)) }
                    repeat(11 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun GridSquare(cell: GridCell, modifier: Modifier) {
    val colors = LrTheme.colors
    val summary = cell.summary
    val fill = when {
        summary == null -> colors.track
        summary.isRestDay -> colors.band
        summary.dueCount > 0 && summary.doneCount == 0 -> colors.row
        else -> colors.iconDisc.copy(alpha = (0.3f + 0.7f * summary.ratio).coerceAtMost(1f))
    }
    Box(
        modifier
            .aspectRatio(1f)
            .background(fill)
            .border(1.dp, colors.rowBorder)
            .then(if (cell.isToday) Modifier.border(2.dp, colors.accent) else Modifier),
    )
}

@Composable
private fun Milestones(seasonDay: Int) {
    val colors = LrTheme.colors
    val marks = listOf(7 to "1주 완주", 21 to "습관 형성", 30 to "한 달", 66 to "시즌 클리어")
    LrPanel(contentPadding = PaddingValues(14.dp)) {
        Text("마일스톤", style = LrTheme.type.bodyStrong, color = colors.textPrimary, modifier = Modifier.padding(bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            marks.forEach { (day, label) ->
                val reached = seasonDay > day
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .border(2.dp, Color(0xE6FFFFFF), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(if (reached) colors.iconDisc else colors.circle),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$day", style = LrTheme.type.numberSmall.copy(fontSize = 15.sp), color = if (reached) colors.onIconDisc else colors.textSecondary)
                    }
                    Text(label, style = LrTheme.type.caption.copy(fontSize = 11.sp), color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Totals(player: PlayerState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Total("최장 연속", "${player.bestStreak}일", Modifier.weight(1f))
        Total("완료 퀘스트", "${player.totalCompletions}", Modifier.weight(1f))
        Total("보스 처치", "${player.bossesDefeated}", Modifier.weight(1f))
    }
}

@Composable
private fun Total(label: String, value: String, modifier: Modifier) {
    LrCard(modifier) {
        Text(label, style = LrTheme.type.caption.copy(fontSize = 11.sp), color = LrTheme.colors.textSecondary)
        Text(value, style = LrTheme.type.number.copy(fontSize = 20.sp), color = LrTheme.colors.textPrimary)
    }
}

@Composable
private fun PastSeasons(player: PlayerState) {
    if (player.seasonRecords.isEmpty()) return
    val colors = LrTheme.colors
    LrPanel(title = "CLEARED SEASONS") {
        player.seasonRecords.reversed().forEach { record ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("시즌 ${record.season}", style = LrTheme.type.bodyStrong, color = colors.textPrimary)
                Text("Lv.${record.levelAtEnd} · EXP ${record.xpGained} · 완벽 ${record.perfectDays}일", style = LrTheme.type.caption, color = colors.textSecondary)
            }
        }
    }
}
