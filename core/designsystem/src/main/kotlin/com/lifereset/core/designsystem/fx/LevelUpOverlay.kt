package com.lifereset.core.designsystem.fx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.lifereset.core.designsystem.component.ConfirmCancelRow
import com.lifereset.core.designsystem.component.DialogWindow
import com.lifereset.core.designsystem.component.SystemWindowAppear
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.LevelUpRecord
import com.lifereset.core.game.model.Stat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 레벨업 연출 (약 2.5초):
 * 막이 깔림 → 빛 고리가 퍼짐 → 시스템 창이 열림 → 'LEVEL UP' 이 흔들리며 등장
 * → 레벨 숫자가 굴러 올라감 → 오른 스탯이 한 칸씩 켜짐. 탭하면 닫힌다.
 */
@Composable
internal fun LevelUpOverlay(record: LevelUpRecord, onDone: () -> Unit) {
    val colors = LrTheme.colors
    val glitch = remember { Animatable(1f) }
    val levelRoll = remember { Animatable((record.newLevel - 1).toFloat()) }
    var revealedStats by remember { mutableIntStateOf(0) }

    LaunchedEffect(record) {
        launch { glitch.animateTo(0f, tween(durationMillis = 420, delayMillis = 300)) }
        delay(500)
        levelRoll.animateTo(record.newLevel.toFloat(), tween(durationMillis = 500))
        repeat(Stat.entries.size) {
            delay(110)
            revealedStats = it + 1
        }
    }

    FxScrim(onTap = onDone) {
        RingBurst(color = Color.White)
        ShardBurst(color = colors.accent, delayMillis = 150)
        SystemWindowAppear(Modifier.padding(24.dp)) {
            DialogWindow(
                title = "LEVEL UP",
                modifier = Modifier.widthIn(max = 340.dp),
                footer = { ConfirmCancelRow(onConfirm = onDone, onCancel = onDone, confirmLabel = "확인", cancelLabel = "닫기") },
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val jitter = (sin(glitch.value * 40f) * 6f * glitch.value).roundToInt()
                    Text(
                        text = "LV ${record.newLevel}",
                        style = LrTheme.type.number.copy(fontSize = 40.sp, letterSpacing = 0.08.em),
                        color = colors.textPrimary,
                        modifier = Modifier.offset { IntOffset(jitter, 0) },
                    )
                    Row(
                        Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("LV ${record.newLevel - 1}", style = LrTheme.type.number.copy(fontSize = 20.sp), color = colors.textTertiary)
                        Icon(LrIcons.Back, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp).graphicsFlip())
                        Text("LV ${levelRoll.value.roundToInt()}", style = LrTheme.type.number.copy(fontSize = 28.sp), color = colors.textPrimary)
                    }
                }
                Text(
                    "이번 레벨에 한 활동대로 능력치가 올랐습니다.",
                    style = LrTheme.type.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                StatGainGrid(record.gains, revealedStats)
                Box(Modifier.size(14.dp))
                if (record.classChanged) {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0x33FFFFFF)).padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("클래스", style = LrTheme.type.caption, color = colors.textSecondary)
                        Text("${record.classBefore.label} → ${record.classAfter.label}", style = LrTheme.type.bodyStrong, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatGainGrid(gains: Map<Stat, Int>, revealed: Int) {
    val colors = LrTheme.colors
    val best = gains.values.maxOrNull() ?: 0
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat.entries.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { colIndex, stat ->
                    val index = rowIndex * 3 + colIndex
                    val gain = gains[stat] ?: 0
                    val highlight = gain == best && best > 1
                    Box(Modifier.weight(1f)) {
                        AnimatedVisibility(
                            visible = index < revealed,
                            enter = fadeIn(tween(160)) + slideInVertically(tween(160)) { it / 2 },
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (highlight) colors.accent.copy(alpha = 0.18f) else colors.row)
                                    .border(1.dp, if (highlight) colors.accent else colors.rowBorder)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stat.code, style = LrTheme.type.code, color = colors.textSecondary)
                                Text("+$gain", style = LrTheme.type.number.copy(fontSize = 16.sp), color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 뒤로 화살표를 뒤집어 '→' 로 쓴다. */
private fun Modifier.graphicsFlip(): Modifier = this.graphicsLayer(rotationZ = 180f)
