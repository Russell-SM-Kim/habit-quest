package com.lifereset.core.protocol.wear

import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.detailText
import kotlinx.serialization.Serializable

/**
 * 폰 <-> 갤럭시 워치 (Wear OS 데이터 레이어) 형식.
 * 폰이 계산·저장의 주인이고, 워치는 화면과 리모컨 역할만 한다.
 *
 * - 폰 -> 워치: [STATE_PATH] DataItem 에 [WearSnapshot] JSON 을 넣는다 (워치가 꺼져 있어도 다음에 받음).
 * - 워치 -> 폰: [COMPLETE_PATH] 메시지로 [WearCompleteRequest] 를 보낸다.
 * - 워치 -> 폰: [REFRESH_PATH] 메시지로 최신 상태를 다시 보내 달라고 한다.
 */
object WearPaths {
    const val STATE_PATH = "/life-reset/state"
    const val STATE_KEY = "snapshot"
    const val COMPLETE_PATH = "/life-reset/complete"
    const val REFRESH_PATH = "/life-reset/refresh"
}

@Serializable
data class WearSnapshot(
    val level: Int,
    val rank: String,
    val playerClass: String,
    val xpInLevel: Int,
    val xpToNext: Int,
    val totalXp: Long,
    val streak: Int,
    val seasonDay: Int,
    val seasonLength: Int,
    val quests: List<WearQuest>,
    val generatedAt: Long,
)

@Serializable
data class WearQuest(
    val questId: String,
    val slotKey: String,
    val title: String,
    val detail: String,
    val done: Boolean,
)

@Serializable
data class WearCompleteRequest(
    val questId: String,
    /** 워치에서는 기본으로 목표량만큼 한 것으로 본다. 초과량은 폰에서 기록 추가. */
    val amount: Double? = null,
)

fun PlayerState.toWearSnapshot(nowMillis: Long) = WearSnapshot(
    level = level,
    rank = rankLabel,
    playerClass = playerClass.label,
    xpInLevel = xpInLevel,
    xpToNext = xpToNext,
    totalXp = totalXp,
    streak = streak,
    seasonDay = season.day,
    seasonLength = season.seasonLength,
    quests = today.slots.map { slot ->
        // 택1 은 실제로 한 퀘스트, 안 했으면 첫 번째 퀘스트를 대표로 보여준다
        val quest = slot.completedQuest ?: slot.primary
        WearQuest(
            questId = quest.id,
            slotKey = slot.key,
            title = if (slot.isChoice && !slot.done) slot.quests.joinToString(" / ") { it.title } else quest.title,
            detail = quest.detailText(season.weekOfGame),
            done = slot.done,
        )
    },
    generatedAt = nowMillis,
)
