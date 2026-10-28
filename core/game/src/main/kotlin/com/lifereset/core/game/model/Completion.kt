package com.lifereset.core.game.model

import java.time.LocalDate
import java.util.UUID

/**
 * 퀘스트 완료 기록. 앱이 저장하는 유일한 '사실'이다.
 * 레벨·스탯·연속 기록은 저장하지 않고, 이 기록들을 처음부터 다시 계산해서 얻는다.
 *
 * - 취소하면 지우지 않고 [voided] = true 로 남긴다 (동기화 때 삭제를 전달하기 위해).
 * - 같은 기록을 고치면 [updatedAt] 이 커진다. 동기화 충돌은 updatedAt 이 큰 쪽이 이긴다.
 */
data class Completion(
    val id: String,
    val questId: String,
    val questTitle: String,
    val date: LocalDate,
    val completedAt: Long,
    val updatedAt: Long,
    // 완료 시점의 퀘스트 규칙 스냅샷
    val slotKey: String,
    val difficulty: Difficulty,
    val weights: StatWeights,
    val category: QuestCategory,
    val targetAmount: Double? = null,
    // 실제 기록
    val amount: Double? = null,
    val won: Boolean = false,
    val voided: Boolean = false,
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        /** 퀘스트 정의로부터 완료 기록을 만든다. */
        fun of(
            quest: QuestDef,
            date: LocalDate,
            nowMillis: Long,
            amount: Double? = null,
            won: Boolean = false,
            id: String = newId(),
        ) = Completion(
            id = id,
            questId = quest.id,
            questTitle = quest.title,
            date = date,
            completedAt = nowMillis,
            updatedAt = nowMillis,
            slotKey = quest.slotKey,
            difficulty = quest.difficulty,
            weights = quest.weights,
            category = quest.category,
            targetAmount = quest.target?.amount,
            amount = amount,
            won = won,
        )
    }
}
