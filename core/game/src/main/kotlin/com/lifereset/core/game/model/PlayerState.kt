package com.lifereset.core.game.model

import com.lifereset.core.game.rules.PlayerClass
import com.lifereset.core.game.rules.Rank
import com.lifereset.core.game.rules.SeasonPosition
import java.time.LocalDate

/**
 * 엔진이 기록을 다시 계산해서 만든 '지금의 나'. 화면은 이것만 보고 그린다.
 * 저장하지 않는다. 언제든 기록에서 다시 만들 수 있다.
 */
data class PlayerState(
    val level: Int,
    val xpInLevel: Int,
    val xpToNext: Int,
    val totalXp: Long,
    val rank: Rank,
    val rankLabel: String,
    val stats: Map<Stat, Int>,
    /** 이번 레벨 동안 쌓인 스탯 경험치 비율 (합 1.0). 다음 레벨업 때 이 비율로 보너스가 나뉜다. */
    val growthShare: Map<Stat, Double>,
    val playerClass: PlayerClass,
    /** 오늘을 기준으로 이어지고 있는 연속 일수 (오늘 이미 기준을 넘었으면 오늘 포함). */
    val streak: Int,
    val bestStreak: Int,
    val shields: Int,
    val season: SeasonPosition,
    val today: TodayState,
    val boss: BossState?,
    val history: List<DaySummary>,
    val levelUps: List<LevelUpRecord>,
    val seasonRecords: List<SeasonRecord>,
    val bossesDefeated: Int,
    val totalCompletions: Int,
) {
    val xpProgress: Float get() = if (xpToNext == 0) 0f else xpInLevel.toFloat() / xpToNext
}

data class TodayState(
    val date: LocalDate,
    val slots: List<QuestSlot>,
    /** 어제 연속 기록이 끊겨서 오늘 회복 퀘스트를 할 수 있는지. */
    val recoveryAvailable: Boolean,
    val recoveryCompletion: Completion?,
    val perfect: Boolean,
) {
    val dueCount: Int get() = slots.size
    val doneCount: Int get() = slots.count { it.done }
}

/** 하루 일정의 한 칸. 택1 그룹이면 quests 가 여러 개다. */
data class QuestSlot(
    val key: String,
    val quests: List<QuestDef>,
    val completion: Completion?,
) {
    val done: Boolean get() = completion != null
    val isChoice: Boolean get() = quests.size > 1
    val primary: QuestDef get() = quests.first()

    /** 완료된 퀘스트 (택1이면 실제로 한 쪽). */
    val completedQuest: QuestDef? get() = completion?.let { c -> quests.firstOrNull { it.id == c.questId } }
}

data class DaySummary(
    val date: LocalDate,
    val dueCount: Int,
    val doneCount: Int,
    val perfect: Boolean,
    /** 연속 기록 기준을 넘었는지. 일정이 없는 날은 true. */
    val success: Boolean,
    val shieldUsed: Boolean,
) {
    val ratio: Float get() = if (dueCount == 0) 0f else doneCount.toFloat() / dueCount
    val isRestDay: Boolean get() = dueCount == 0
}

data class LevelUpRecord(
    val date: LocalDate,
    val newLevel: Int,
    val gains: Map<Stat, Int>,
    val rankBefore: Rank,
    val rankAfter: Rank,
    val classBefore: PlayerClass,
    val classAfter: PlayerClass,
) {
    val rankChanged: Boolean get() = rankBefore != rankAfter
    val classChanged: Boolean get() = classBefore != classAfter
}

data class SeasonRecord(
    val season: Int,
    val start: LocalDate,
    val end: LocalDate,
    val xpGained: Long,
    val levelAtEnd: Int,
    val perfectDays: Int,
    val successDays: Int,
)

data class BossState(
    val name: String,
    val weekStart: LocalDate,
    val maxHp: Int,
    val damage: Int,
    val defeated: Boolean,
) {
    val hpLeft: Int get() = (maxHp - damage).coerceAtLeast(0)
}
