package com.lifereset.feature.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.repository.GameRepository
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.PanelBand
import com.lifereset.core.designsystem.component.SkewGauge
import com.lifereset.core.designsystem.component.ThinBar
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.Stat

@Composable
fun StatusRoute(
    game: GameRepository,
    settings: SettingsRepository,
    viewModel: StatusViewModel = viewModel(factory = StatusViewModel.factory(game, settings)),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val player = ui.player ?: return
    StatusScreen(player, ui.name)
}

@Composable
fun StatusScreen(player: PlayerState, name: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = LrTheme.Dimens.screenPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroHeader(
            title = "Status",
            caption = "${player.playerClass.label} · ${player.rankLabel} 랭크 · ${player.rank.title}",
        )
        ProfilePanel(player, name)
        AbilityPanel(player)
        GrowthPanel(player)
        RecentLevelUps(player)
    }
}

@Composable
private fun ProfilePanel(player: PlayerState, name: String) {
    val colors = LrTheme.colors
    LrPanel(title = "STATUS") {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .border(3.dp, Color(0xE6FFFFFF), CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(colors.iconDisc),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LV", style = LrTheme.type.code, color = colors.onIconDisc)
                    Text("${player.level}", style = LrTheme.type.number.copy(lineHeight = 30.sp), color = colors.onIconDisc)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row { Info("이름", name, Modifier.weight(1f)); Info("클래스", player.playerClass.label, Modifier.weight(1f)) }
                Row {
                    Info("랭크", "${player.rankLabel} · ${player.rank.title}", Modifier.weight(1f))
                    Info("누적 EXP", "%,d".format(player.totalXp), Modifier.weight(1f))
                }
            }
        }
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("EXP", style = LrTheme.type.code, color = colors.textSecondary)
            SkewGauge(player.xpProgress, Modifier.weight(1f))
            Text("다음 레벨까지 ${player.xpToNext - player.xpInLevel}", style = LrTheme.type.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun Info(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = LrTheme.type.caption.copy(fontSize = 11.sp), color = LrTheme.colors.textTertiary)
        Text(value, style = LrTheme.type.bodyStrong, color = LrTheme.colors.textPrimary)
    }
}

@Composable
private fun AbilityPanel(player: PlayerState) {
    val colors = LrTheme.colors
    val max = (player.stats.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val top = Stat.entries.maxByOrNull { player.stats[it] ?: 0 }
    LrPanel(title = "ABILITY") {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(LrTheme.Dimens.rowGap)) {
            Stat.entries.forEach { stat ->
                val value = player.stats[stat] ?: 0
                val gold = stat == top && player.level >= 2
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(if (gold) colors.accent else colors.row)
                        .border(1.dp, if (gold) colors.accent else colors.rowBorder)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val text = if (gold) colors.onAccent else colors.textPrimary
                    Text(stat.code, style = LrTheme.type.code.copy(fontSize = 13.sp), color = text, modifier = Modifier.width(36.dp))
                    Text(stat.label, style = LrTheme.type.caption, color = if (gold) colors.onAccent else colors.textSecondary, modifier = Modifier.width(52.dp))
                    ThinBar(
                        progress = value.toFloat() / max,
                        modifier = Modifier.weight(1f),
                        color = if (gold) Color.White else colors.iconDisc,
                    )
                    Text("$value", style = LrTheme.type.number.copy(fontSize = 18.sp), color = text, modifier = Modifier.width(36.dp))
                }
            }
        }
        PanelBand {
            Text(
                "레벨업하면 모든 능력치 +1, 보너스 ${player.rank.bonusPoints}점은 아래 비율대로 자동 분배됩니다.",
                style = LrTheme.type.caption,
                color = colors.textPrimary,
            )
        }
    }
}

/** 다음 레벨업 때 보너스가 어디로 갈지 미리 보여주는 막대. */
@Composable
private fun GrowthPanel(player: PlayerState) {
    val colors = LrTheme.colors
    val shares = player.growthShare.entries.filter { it.value > 0.0 }.sortedByDescending { it.value }
    LrPanel(contentPadding = PaddingValues(14.dp)) {
        Text("다음 레벨업 예상 분배", style = LrTheme.type.bodyStrong, color = colors.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
        if (shares.isEmpty()) {
            Text("이번 레벨에서 아직 완료한 퀘스트가 없습니다.", style = LrTheme.type.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
            return@LrPanel
        }
        Row(Modifier.fillMaxWidth().height(14.dp).padding(top = 0.dp).border(1.dp, colors.trackBorder)) {
            shares.forEachIndexed { index, (_, share) ->
                Box(Modifier.weight(share.toFloat()).fillMaxHeight().background(colors.iconDisc.copy(alpha = shadeFor(index))))
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            shares.forEachIndexed { index, (stat, share) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).background(colors.iconDisc.copy(alpha = shadeFor(index))).border(1.dp, colors.trackBorder))
                    Text("${stat.code} ${(share * 100).toInt()}%", style = LrTheme.type.caption, color = colors.textPrimary)
                }
            }
        }
    }
}

private fun shadeFor(index: Int): Float = listOf(1f, 0.7f, 0.45f, 0.28f, 0.18f, 0.1f)[index.coerceAtMost(5)]

@Composable
private fun RecentLevelUps(player: PlayerState) {
    val recent = player.levelUps.takeLast(5).reversed()
    if (recent.isEmpty()) return
    val colors = LrTheme.colors
    LrPanel(title = "LOG") {
        recent.forEach { record ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("LV ${record.newLevel}", style = LrTheme.type.numberSmall, color = colors.textPrimary)
                Text(
                    record.gains.entries.filter { it.value > 1 }.sortedByDescending { it.value }
                        .joinToString(" ") { "${it.key.code}+${it.value}" },
                    style = LrTheme.type.caption,
                    color = colors.textSecondary,
                )
                Text("${record.date.monthValue}.${record.date.dayOfMonth}", style = LrTheme.type.caption, color = colors.textTertiary)
            }
        }
    }
}
