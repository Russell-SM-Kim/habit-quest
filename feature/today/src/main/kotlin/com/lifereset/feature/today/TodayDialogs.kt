package com.lifereset.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.component.LrField
import com.lifereset.core.designsystem.component.LrTextField
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.component.SegmentedRow
import com.lifereset.core.designsystem.component.SystemDialog
import com.lifereset.core.designsystem.component.ToggleChip
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestSlot
import com.lifereset.core.game.model.formatAmount
import com.lifereset.core.game.rules.GameRules

/** 완료 전: 택1 고르기 + 실제로 한 양 + 승패. */
@Composable
internal fun CompleteDialog(
    slot: QuestSlot,
    onConfirm: (quest: QuestDef, amount: Double?, won: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember(slot) { mutableStateOf(slot.primary) }
    var amountText by remember(chosen) { mutableStateOf(chosen.target?.amount?.formatAmount().orEmpty()) }
    var won by remember(chosen) { mutableStateOf(false) }
    val target = chosen.target
    val amount = amountText.toDoubleOrNull()

    SystemDialog(
        title = "QUEST CLEAR",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(chosen, amount, won) },
        confirmLabel = "완료",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (slot.isChoice) {
                LrField("오늘 한 것") {
                    SegmentedRow(options = slot.quests, selected = chosen, onSelect = { chosen = it }, label = { it.title })
                }
            } else {
                Text(chosen.title, style = LrTheme.type.title, color = LrTheme.colors.textPrimary)
            }
            if (target != null) {
                LrField(
                    label = "실제로 한 양 (${target.unit.label})",
                    hint = overachieveHint(amount, target.amount),
                ) {
                    LrTextField(value = amountText, onValueChange = { amountText = it }, number = true)
                }
            }
            if (chosen.tracksWin) {
                ToggleChip(text = if (won) "경기 승리 ✓" else "경기에서 이겼다면 탭", selected = won, onClick = { won = !won })
            }
        }
    }
}

/** 완료 후: 더 한 양을 추가하거나, 완료를 취소. */
@Composable
internal fun CompletedDialog(
    slot: QuestSlot,
    onAddAmount: (completionId: String, extra: Double) -> Unit,
    onUndo: (completionId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val completion = slot.completion ?: return
    val quest = slot.completedQuest ?: slot.primary
    val target = quest.target
    var extraText by remember { mutableStateOf("") }
    val extra = extraText.toDoubleOrNull()

    SystemDialog(
        title = "RECORD",
        onDismiss = onDismiss,
        onConfirm = if (target != null && extra != null && extra > 0) {
            { onAddAmount(completion.id, extra) }
        } else {
            null
        },
        confirmLabel = "추가",
        dismissLabel = "닫기",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(quest.title, style = LrTheme.type.title, color = LrTheme.colors.textPrimary)
            if (target != null) {
                val current = completion.amount ?: target.amount
                Text(
                    "지금 기록: ${current.formatAmount()}${target.unit.label} (목표 ${target.format()})",
                    style = LrTheme.type.caption,
                    color = LrTheme.colors.textSecondary,
                )
                LrField(
                    label = "더 한 양 (${target.unit.label})",
                    hint = overachieveHint(current + (extra ?: 0.0), target.amount),
                ) {
                    LrTextField(value = extraText, onValueChange = { extraText = it }, number = true, placeholder = "예: 30")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LrWideButton(text = "완료 취소", onClick = { onUndo(completion.id) })
            }
        }
    }
}

private fun overachieveHint(amount: Double?, target: Double): String {
    if (amount == null || amount <= target) return "목표를 넘기면 넘긴 만큼 EXP 추가 (최대 2배)"
    val ratio = ((amount - target) / target).coerceAtMost(GameRules.Default.overachieveCap)
    return "초과 달성 보너스 +${(ratio * 100).toInt()}%"
}
