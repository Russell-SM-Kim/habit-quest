package com.lifereset.feature.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lifereset.core.data.repository.QuestRepository
import com.lifereset.core.data.time.TimeProvider
import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestTarget
import com.lifereset.core.game.model.QuestUnit
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.model.formatAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID

/** 퀘스트 편집 폼. 사용자가 고치는 값만 담고, 저장할 때 QuestDef 로 바꾼다. */
data class QuestForm(
    val title: String = "",
    val unit: QuestUnit? = null,
    val amountText: String = "",
    /** 고른 순서대로. 첫 번째가 주 능력치(70%), 두 번째가 보조(30%). */
    val stats: List<Stat> = emptyList(),
    val difficulty: Difficulty = Difficulty.NORMAL,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val exercise: Boolean = false,
    val tracksWin: Boolean = false,
    val note: String = "",
) {
    val errors: List<String>
        get() = buildList {
            if (title.isBlank()) add("이름을 입력하세요")
            if (stats.isEmpty()) add("능력치를 하나 이상 고르세요")
            if (days.isEmpty()) add("요일을 하나 이상 고르세요")
            if (unit != null && (amountText.toDoubleOrNull() ?: 0.0) <= 0.0) add("목표량을 숫자로 입력하세요")
        }
}

data class QuestEditUiState(
    val isNew: Boolean = true,
    val form: QuestForm = QuestForm(),
    val showErrors: Boolean = false,
    val finished: Boolean = false,
)

class QuestEditViewModel(
    private val quests: QuestRepository,
    private val time: TimeProvider,
    private val questId: String?,
) : ViewModel() {

    private val _ui = MutableStateFlow(QuestEditUiState(isNew = questId == null))
    val ui: StateFlow<QuestEditUiState> = _ui.asStateFlow()

    /** 편집 중인 원본. 폼에 없는 값(택1 그룹, 프로그램, 생성일)을 보존하기 위해 들고 있는다. */
    private var original: QuestDef? = null

    init {
        if (questId != null) {
            viewModelScope.launch {
                val quest = quests.get(questId) ?: return@launch
                original = quest
                _ui.update { it.copy(form = quest.toForm()) }
            }
        }
    }

    fun edit(transform: (QuestForm) -> QuestForm) {
        _ui.update { it.copy(form = transform(it.form)) }
    }

    fun toggleStat(stat: Stat) = edit { form ->
        val next = when {
            stat in form.stats -> form.stats - stat
            form.stats.size >= 2 -> listOf(form.stats.last(), stat)
            else -> form.stats + stat
        }
        form.copy(stats = next)
    }

    fun toggleDay(day: DayOfWeek) = edit { form ->
        form.copy(days = if (day in form.days) form.days - day else form.days + day)
    }

    fun save() {
        val form = _ui.value.form
        if (form.errors.isNotEmpty()) {
            _ui.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            val base = original
            val quest = QuestDef(
                id = base?.id ?: "q-${UUID.randomUUID()}",
                title = form.title.trim(),
                difficulty = form.difficulty,
                weights = weightsOf(form.stats),
                days = form.days,
                createdOn = base?.createdOn ?: time.today(),
                target = form.unit?.let { QuestTarget(form.amountText.toDouble(), it) },
                note = form.note.trim(),
                category = if (form.exercise) QuestCategory.EXERCISE else QuestCategory.HABIT,
                choiceGroup = base?.choiceGroup,
                tracksWin = form.tracksWin,
                programId = base?.programId,
                archivedOn = base?.archivedOn,
                sortOrder = base?.sortOrder ?: quests.nextSortOrder(),
            )
            quests.save(quest)
            _ui.update { it.copy(finished = true) }
        }
    }

    fun archive() {
        val id = original?.id ?: return
        viewModelScope.launch {
            quests.archive(id)
            _ui.update { it.copy(finished = true) }
        }
    }

    private fun weightsOf(stats: List<Stat>): Map<Stat, Double> = when (stats.size) {
        1 -> mapOf(stats[0] to 1.0)
        else -> mapOf(stats[0] to 0.7, stats[1] to 0.3)
    }

    private fun QuestDef.toForm() = QuestForm(
        title = title,
        unit = target?.unit,
        amountText = target?.amount?.formatAmount().orEmpty(),
        stats = weights.entries.sortedByDescending { it.value }.map { it.key }.take(2),
        difficulty = difficulty,
        days = days,
        exercise = category == QuestCategory.EXERCISE,
        tracksWin = tracksWin,
        note = note,
    )

    companion object {
        fun factory(quests: QuestRepository, time: TimeProvider, questId: String?): ViewModelProvider.Factory =
            viewModelFactory { initializer { QuestEditViewModel(quests, time, questId) } }
    }
}
