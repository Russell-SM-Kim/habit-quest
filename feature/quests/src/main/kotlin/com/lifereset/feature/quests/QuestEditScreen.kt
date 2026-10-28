package com.lifereset.feature.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.repository.QuestRepository
import com.lifereset.core.data.time.TimeProvider
import com.lifereset.core.designsystem.component.CircleToggle
import com.lifereset.core.designsystem.component.ConfirmCancelRow
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.LrField
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.LrTextField
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.component.SegmentedRow
import com.lifereset.core.designsystem.component.ToggleChip
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.QuestUnit
import com.lifereset.core.game.model.Stat
import java.time.DayOfWeek

@Composable
fun QuestEditRoute(
    quests: QuestRepository,
    time: TimeProvider,
    questId: String?,
    onFinished: () -> Unit,
    viewModel: QuestEditViewModel = viewModel(
        key = "quest-edit-$questId",
        factory = QuestEditViewModel.factory(quests, time, questId),
    ),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(ui.finished) { if (ui.finished) onFinished() }
    QuestEditScreen(
        ui = ui,
        onEdit = viewModel::edit,
        onToggleStat = viewModel::toggleStat,
        onToggleDay = viewModel::toggleDay,
        onSave = viewModel::save,
        onArchive = viewModel::archive,
        onCancel = onFinished,
    )
}

private val DAY_LABELS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 단위 선택지. null = 수치 목표 없음 (체크만). */
private val UNIT_OPTIONS: List<QuestUnit?> = listOf(null) + QuestUnit.entries

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestEditScreen(
    ui: QuestEditUiState,
    onEdit: ((QuestForm) -> QuestForm) -> Unit,
    onToggleStat: (Stat) -> Unit,
    onToggleDay: (DayOfWeek) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val form = ui.form
    val colors = LrTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(LrTheme.Dimens.screenPadding),
    ) {
        HeroHeader(title = if (ui.isNew) "New Quest" else "Edit Quest", caption = "한 번 만들면 고른 요일마다 오늘 화면에 자동으로 나옵니다")
        LrPanel(title = if (ui.isNew) "NEW QUEST" else "EDIT QUEST", modifier = Modifier.padding(top = 10.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LrField("퀘스트 이름") {
                    LrTextField(form.title, { v -> onEdit { it.copy(title = v) } }, placeholder = "예: 독서")
                }

                LrField("목표 단위") {
                    SegmentedRow(
                        options = UNIT_OPTIONS,
                        selected = form.unit,
                        onSelect = { unit -> onEdit { it.copy(unit = unit) } },
                        label = { it?.label ?: "없음" },
                    )
                }
                if (form.unit != null) {
                    LrField("목표량 (${form.unit.label})", hint = "목표를 넘기면 넘긴 비율만큼 EXP 추가 (최대 2배)") {
                        LrTextField(form.amountText, { v -> onEdit { it.copy(amountText = v) } }, number = true)
                    }
                } else {
                    LrField("설명 (선택)") {
                        LrTextField(form.note, { v -> onEdit { it.copy(note = v) } }, placeholder = "예: 07:00 전에 체크")
                    }
                }

                LrField("오르는 능력치 (최대 2개)", hint = "먼저 고른 능력치가 70%, 두 번째가 30%") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Stat.entries.forEach { stat ->
                            val order = form.stats.indexOf(stat)
                            ToggleChip(
                                text = "${stat.code} ${stat.label}" + if (order >= 0) " ${order + 1}" else "",
                                selected = order >= 0,
                                onClick = { onToggleStat(stat) },
                            )
                        }
                    }
                }

                LrField("난이도") {
                    SegmentedRow(
                        options = Difficulty.entries,
                        selected = form.difficulty,
                        onSelect = { d -> onEdit { it.copy(difficulty = d) } },
                        label = { it.label },
                        subLabel = { "${it.baseXp} EXP" },
                    )
                }

                LrField("반복 요일") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DayOfWeek.entries.forEachIndexed { i, day ->
                            CircleToggle(DAY_LABELS[i], selected = day in form.days, onClick = { onToggleDay(day) })
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToggleChip("운동 (주간 보스 데미지)", form.exercise, { onEdit { it.copy(exercise = !it.exercise) } })
                    ToggleChip("승패 기록", form.tracksWin, { onEdit { it.copy(tracksWin = !it.tracksWin) } })
                }

                if (ui.showErrors && form.errors.isNotEmpty()) {
                    Text(form.errors.joinToString("\n"), style = LrTheme.type.caption, color = colors.danger)
                }
            }
            ConfirmCancelRow(onConfirm = onSave, onCancel = onCancel, confirmLabel = "저장")
        }
        if (!ui.isNew) {
            LrWideButton(
                text = "보관하기 (오늘부터 안 나옴, 기록은 유지)",
                onClick = onArchive,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
