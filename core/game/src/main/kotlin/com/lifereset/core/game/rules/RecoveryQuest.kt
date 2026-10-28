package com.lifereset.core.game.rules

import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.Stat
import java.time.LocalDate

/**
 * 회복 퀘스트. 연속 기록이 끊긴 다음날 하루 동안만 나타나고,
 * 완료하면 끊기기 전 연속 기록이 복구된다. 벌 대신 만회 기회.
 */
object RecoveryQuest {
    const val ID = "recovery"
    const val SLOT_KEY = "recovery"

    fun definition(date: LocalDate): QuestDef = QuestDef(
        id = ID,
        title = "회복 퀘스트",
        note = "스쿼트 50회 + 명상 10분",
        difficulty = Difficulty.HARD,
        weights = mapOf(Stat.WIL to 0.6, Stat.STR to 0.4),
        days = emptySet(), // 일정이 아니라 조건으로 등장한다
        createdOn = date,
        category = QuestCategory.HABIT,
        choiceGroup = SLOT_KEY,
    )
}
