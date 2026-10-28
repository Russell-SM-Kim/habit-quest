package com.lifereset.feature.today.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.PanelBand
import com.lifereset.core.designsystem.component.QuestRow
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.QuestSlot
import com.lifereset.core.game.model.detailText
import com.lifereset.core.game.model.formatAmount
import com.lifereset.core.game.model.statCodes
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

@Composable
internal fun DailyQuestPanel(
    player: PlayerState,
    onSlotTap: (QuestSlot) -> Unit,
    onSlotLongPress: (QuestSlot) -> Unit,
) {
    val colors = LrTheme.colors
    val today = player.today
    // 금색 강조는 '다음에 할 퀘스트' 하나에만
    val next = today.slots.firstOrNull { !it.done }
    LrPanel(
        title = "DAILY QUEST",
        headerEnd = { MidnightCountdown() },
    ) {
        if (today.slots.isEmpty()) {
            Text(
                "오늘은 예정된 퀘스트가 없는 휴식일입니다.",
                style = LrTheme.type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(16.dp),
            )
        }
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(LrTheme.Dimens.rowGap),
        ) {
            today.slots.forEach { slot ->
                val view = slot.toRowText(player.season.weekOfGame)
                QuestRow(
                    title = view.title,
                    subtitle = view.subtitle,
                    trailing = view.trailing,
                    done = slot.done,
                    onClick = { onSlotTap(slot) },
                    onLongClick = { onSlotLongPress(slot) },
                    code = view.code,
                    highlighted = slot == next,
                )
            }
        }
        PanelBand {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "완료 ${today.doneCount} / ${today.dueCount}",
                    style = LrTheme.type.bodyStrong.copy(fontSize = LrTheme.type.caption.fontSize),
                    color = colors.textPrimary,
                )
                Text(
                    if (today.perfect) "완벽한 하루 달성" else "전부 끝내면 보너스 EXP",
                    style = LrTheme.type.caption,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** code = 원형 아이콘에 들어갈 주 능력치 코드 (STR, INT …). */
private data class RowText(val title: String, val subtitle: String, val trailing: String, val code: String)

private fun QuestSlot.toRowText(weekOfGame: Int): RowText {
    val completion = completion
    val quest = completedQuest ?: primary
    val title = if (isChoice && completion == null) quests.joinToString(" / ") { it.title } else quest.title
    val detail = quest.detailText(weekOfGame)
    val subtitle = buildString {
        append(quest.statCodes())
        if (detail.isNotEmpty() && quest.target == null) append(" · ").append(detail)
        if (completion?.amount != null && quest.target != null) {
            append(" · ").append(completion.amount!!.formatAmount()).append(quest.target!!.unit.label).append(" 기록")
        }
        if (quest.tracksWin && completion?.won == true) append(" · 승리")
    }
    val trailing = quest.target?.format() ?: "체크"
    val code = quest.weights.maxByOrNull { it.value }?.key?.code.orEmpty()
    return RowText(title, subtitle, trailing, code)
}

/** 자정까지 남은 시간 (일일 퀘스트 초기화). */
@Composable
private fun MidnightCountdown() {
    var text by remember { mutableStateOf(remaining()) }
    LaunchedEffect(Unit) {
        while (true) {
            text = remaining()
            delay(1_000)
        }
    }
    Text(text, style = LrTheme.type.numberSmall, color = LrTheme.colors.textSecondary)
}

private fun remaining(): String {
    val now = LocalDateTime.now()
    val seconds = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).seconds
    return "%02d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}
