package com.lifereset.feature.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifereset.core.data.repository.QuestRepository
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.IconDisc
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.statCodes
import java.time.DayOfWeek

/** 퀘스트 목록. 상태가 단순해서 ViewModel 없이 저장소 Flow 를 바로 읽는다. */
@Composable
fun QuestListRoute(
    quests: QuestRepository,
    onEdit: (questId: String) -> Unit,
    onAdd: () -> Unit,
) {
    val list by quests.quests.collectAsStateWithLifecycle(initialValue = emptyList())
    QuestListScreen(list, onEdit, onAdd)
}

@Composable
fun QuestListScreen(
    quests: List<QuestDef>,
    onEdit: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showArchived by remember { mutableStateOf(false) }
    val (archived, active) = quests.partition { it.isArchived }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(LrTheme.Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroHeader(title = "Quests", caption = "활성 ${active.size}개 · 탭해서 수정")
        LrPanel(title = "QUEST LIST") {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(LrTheme.Dimens.rowGap)) {
                active.forEach { QuestListRow(it, onEdit) }
            }
        }
        LrWideButton(text = "새 퀘스트", onClick = onAdd, icon = LrIcons.Plus, filled = true)
        if (archived.isNotEmpty()) {
            LrWideButton(
                text = if (showArchived) "보관한 퀘스트 숨기기" else "보관한 퀘스트 ${archived.size}개 보기",
                onClick = { showArchived = !showArchived },
            )
            if (showArchived) {
                LrPanel(title = "ARCHIVED") {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(LrTheme.Dimens.rowGap)) {
                        archived.forEach { QuestListRow(it, onEdit) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestListRow(quest: QuestDef, onEdit: (String) -> Unit) {
    val colors = LrTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.row)
            .border(1.dp, colors.rowBorder)
            .clickable(role = Role.Button) { onEdit(quest.id) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconDisc(icon = LrIcons.Edit)
        Column(Modifier.weight(1f)) {
            Text(quest.title, style = LrTheme.type.rowTitle, color = colors.textPrimary)
            Text(
                listOfNotNull(daysText(quest.days), quest.target?.format(), quest.statCodes()).joinToString(" · "),
                style = LrTheme.type.caption,
                color = colors.textSecondary,
            )
        }
        Text("${quest.difficulty.baseXp} EXP", style = LrTheme.type.numberSmall, color = colors.textSecondary)
    }
}

private fun daysText(days: Set<DayOfWeek>): String {
    val labels = listOf("월", "화", "수", "목", "금", "토", "일")
    return when (days.size) {
        7 -> "매일"
        0 -> "요일 없음"
        else -> DayOfWeek.entries.filter { it in days }.joinToString(" ") { labels[it.ordinal] }
    }
}
