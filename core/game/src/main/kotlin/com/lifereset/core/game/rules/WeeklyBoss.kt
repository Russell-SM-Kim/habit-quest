package com.lifereset.core.game.rules

import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 주간 보스. 매주 월요일 등장하고, 그 주 운동 퀘스트 1회 완료 = 데미지 1.
 * HP = 그 주에 예정된 운동 횟수 - 1 (한 번은 놓쳐도 잡을 수 있게). 예정이 2회 이하면 그 횟수 그대로.
 */
object WeeklyBoss {

    val names = listOf(
        "미루기 슬라임",
        "침대의 망령",
        "무한 스크롤 리치",
        "야식 오우거",
        "핑계의 군주",
        "작심삼일 골렘",
        "소파 드래곤",
        "알람 끄기 고블린",
    )

    fun weekStartOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 운동 퀘스트가 하나도 없는 주에는 보스가 없다 (null). */
    fun spawn(weekStart: LocalDate, gameStart: LocalDate, quests: List<QuestDef>): BossSpawn? {
        val exerciseSlots = (0L..6L)
            .map { weekStart.plusDays(it) }
            .filterNot { it.isBefore(gameStart) }
            .sumOf { day ->
                quests.filter { it.category == QuestCategory.EXERCISE && it.isDueOn(day) }
                    .map { it.slotKey }
                    .distinct()
                    .size
            }
        if (exerciseSlots == 0) return null
        val weekIndex = ChronoUnit.WEEKS.between(weekStartOf(gameStart), weekStart).toInt()
        return BossSpawn(
            name = names[Math.floorMod(weekIndex, names.size)],
            weekStart = weekStart,
            maxHp = if (exerciseSlots >= 3) exerciseSlots - 1 else exerciseSlots,
        )
    }
}

data class BossSpawn(val name: String, val weekStart: LocalDate, val maxHp: Int)
