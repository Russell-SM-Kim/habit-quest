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
 * 처음 실행할 때 넣는 기본 퀘스트 (기획서 '퀘스트 설계' 표).
 * id 는 고정 문자열이라 폰을 바꿔도 서버 기록과 이어진다.
 */
object DefaultQuests {
    const val CARDIO_GROUP = "cardio"

    private val EVERY_DAY: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    private val STRENGTH_DAYS = setOf(MONDAY, WEDNESDAY, FRIDAY)
    private val CARDIO_DAYS = setOf(TUESDAY, THURSDAY, SATURDAY)

    fun create(startDate: LocalDate): List<QuestDef> = listOf(
        QuestDef(
            id = "q-wake", title = "7시 전 기상", note = "07:00 전에 체크",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.WIL to 1.0),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 0,
        ),
        QuestDef(
            id = "q-water", title = "아침 물 한 잔", note = "일어나서 바로",
            difficulty = Difficulty.EASY, weights = mapOf(Stat.VIT to 1.0),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 1,
        ),
        QuestDef(
            id = "q-strength", title = "맨몸 근력 서킷",
            difficulty = Difficulty.HARD, weights = mapOf(Stat.STR to 0.8, Stat.VIT to 0.2),
            days = STRENGTH_DAYS, createdOn = startDate, sortOrder = 2,
            category = QuestCategory.EXERCISE, programId = StrengthProgram.ID,
        ),
        QuestDef(
            id = "q-table-tennis", title = "탁구",
            target = QuestTarget(40.0, QuestUnit.MINUTES),
            difficulty = Difficulty.HARD, weights = mapOf(Stat.AGI to 0.6, Stat.END to 0.4),
            days = CARDIO_DAYS, createdOn = startDate, sortOrder = 3,
            category = QuestCategory.EXERCISE, choiceGroup = CARDIO_GROUP, tracksWin = true,
        ),
        QuestDef(
            id = "q-running", title = "러닝",
            target = QuestTarget(3.0, QuestUnit.KM),
            difficulty = Difficulty.HARD, weights = mapOf(Stat.END to 0.8, Stat.VIT to 0.2),
            days = CARDIO_DAYS, createdOn = startDate, sortOrder = 4,
            category = QuestCategory.EXERCISE, choiceGroup = CARDIO_GROUP,
        ),
        QuestDef(
            id = "q-stretch", title = "스트레칭",
            target = QuestTarget(10.0, QuestUnit.MINUTES),
            difficulty = Difficulty.EASY, weights = mapOf(Stat.VIT to 1.0),
            days = setOf(SUNDAY), createdOn = startDate, sortOrder = 5,
        ),
        QuestDef(
            id = "q-reading", title = "독서",
            target = QuestTarget(20.0, QuestUnit.MINUTES),
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.INT to 1.0),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 6,
        ),
        QuestDef(
            id = "q-meditation", title = "명상",
            target = QuestTarget(10.0, QuestUnit.MINUTES),
            difficulty = Difficulty.EASY, weights = mapOf(Stat.WIL to 0.7, Stat.INT to 0.3),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 7,
        ),
        QuestDef(
            id = "q-no-phone", title = "취침 1시간 전 폰 금지", note = "22:00 이후",
            difficulty = Difficulty.NORMAL, weights = mapOf(Stat.WIL to 0.5, Stat.VIT to 0.5),
            days = EVERY_DAY, createdOn = startDate, sortOrder = 8,
        ),
    )
}
