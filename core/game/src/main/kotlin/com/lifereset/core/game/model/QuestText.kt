package com.lifereset.core.game.model

import com.lifereset.core.game.rules.StrengthProgram

/**
 * 퀘스트 아래에 보여줄 목표 설명. 폰과 워치가 같은 문구를 쓰도록 여기서 만든다.
 * @param weekOfGame 게임 시작 후 몇 주차인지 (근력 단계 계산용)
 */
fun QuestDef.detailText(weekOfGame: Int): String = when {
    programId == StrengthProgram.ID -> StrengthProgram.phaseForWeek(weekOfGame).summary()
    target != null -> target.format()
    else -> note
}

/** "STR · VIT" 처럼 오르는 스탯 코드. */
fun QuestDef.statCodes(): String =
    weights.entries.sortedByDescending { it.value }.joinToString(" · ") { it.key.code }
