package com.lifereset.core.game.seed

import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestTarget
import com.lifereset.core.game.model.QuestUnit
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.rules.StrengthProgram
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

/**
 * 기본 퀘스트 = 내 하루 루틴.
 *  기상 → 물 → 음료수 X → 공부(토익스피킹·임베디드) → 운동 → 폰 X → 취침
 * id 는 고정 문자열이라 폰을 바꿔도 서버 기록과 이어지고, '내 루틴으로 바꾸기'를 다시 눌러도 중복되지 않는다.
 * 루틴을 바꾸고 싶으면 이 목록만 고치면 된다.
 */
object DefaultQuests {
    const val CARDIO_GROUP = "cardio"

    /** 예전 기본 퀘스트 중 지금 루틴에서 뺀 것. '내 루틴으로 바꾸기' 때 보관 처리한다. */
    val RETIRED_IDS: Set<String> = setOf("q-reading", "q-meditation")

    private val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    private val STUDY_DAYS = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
    private val STRENGTH_DAYS = setOf(MONDAY, WEDNESDAY, FRIDAY)
    private val CARDIO_DAYS = setOf(TUESDAY, THURSDAY, SATURDAY)

    fun create(startDate: LocalDate): List<QuestDef> = listOf(
        QuestDef(
            id = "q-wake", title = "07:00 기상", note = "23:30 취침 → 07:00 기상 (7시간 30분)",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.WIL to 1.0),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 0,
        ),
        QuestDef(
            id = "q-water", title = "물 마시기",
            target = QuestTarget(1.5, QuestUnit.LITERS),
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.VIT to 1.0),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 1,
        ),
        QuestDef(
            id = "q-no-soda", title = "음료수 안 마시기", note = "물·차·블랙커피만",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.VIT to 0.7, Stat.WIL to 0.3),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 2,
        ),
        QuestDef(
            id = "q-toeic", title = "토익스피킹",
            target = QuestTarget(2.0, QuestUnit.HOURS), note = "아침 공부 1시간 + 저녁 복습 1시간",
            difficulty = Difficulty.HARD, weights = mapOf(Stat.INT to 0.7, Stat.WIL to 0.3),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 3,
        ),
        QuestDef(
            id = "q-embedded", title = "임베디드 공부",
            target = QuestTarget(3.0, QuestUnit.HOURS),
            difficulty = Difficulty.HARD, weights = mapOf(Stat.INT to 0.7, Stat.WIL to 0.3),
            days = STUDY_DAYS, createdOn = startDate, sortOrder = 4,
        ),
        QuestDef(
            id = "q-strength", title = "맨몸 근력 서킷",
            difficulty = Difficulty.HARD, weights = mapOf(Stat.STR to 0.8, Stat.VIT to 0.2),
            days = STRENGTH_DAYS, createdOn = startDate, sortOrder = 5,
            category = QuestCategory.EXERCISE, programId = StrengthProgram.ID,
        ),
        QuestDef(
            id = "q-table-tennis", title = "탁구",
            target = QuestTarget(40.0, QuestUnit.MINUTES),
            difficulty = Difficulty.HARD, weights = mapOf(Stat.AGI to 0.6, Stat.END to 0.4),
            days = CARDIO_DAYS, createdOn = startDate, sortOrder = 6,
            category = QuestCategory.EXERCISE, choiceGroup = CARDIO_GROUP, tracksWin = true,
        ),
        QuestDef(
            id = "q-running", title = "러닝",
            target = QuestTarget(3.0, QuestUnit.KM),
            difficulty = Difficulty.HARD, weights = mapOf(Stat.END to 0.8, Stat.VIT to 0.2),
            days = CARDIO_DAYS, createdOn = startDate, sortOrder = 7,
            category = QuestCategory.EXERCISE, choiceGroup = CARDIO_GROUP,
        ),
        QuestDef(
            id = "q-stretch", title = "스트레칭",
            target = QuestTarget(10.0, QuestUnit.MINUTES),
            difficulty = Difficulty.EASY, weights = mapOf(Stat.VIT to 1.0),
            days = setOf(SUNDAY), createdOn = startDate, sortOrder = 8,
        ),
        QuestDef(
            id = "q-no-phone", title = "22:30 이후 폰 안 보기", note = "취침 1시간 전",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.WIL to 0.7, Stat.VIT to 0.3),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 9,
        ),
        QuestDef(
            id = "q-sleep", title = "23:30 전 취침",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.VIT to 0.7, Stat.WIL to 0.3),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 10,
        ),
    )
}
