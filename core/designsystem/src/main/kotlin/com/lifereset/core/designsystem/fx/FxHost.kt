package com.lifereset.core.designsystem.fx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.lifereset.core.designsystem.sound.FxSounds
import com.lifereset.core.designsystem.sound.Sfx
import com.lifereset.core.game.engine.Fx
import kotlinx.coroutines.flow.Flow

/**
 * 연출 재생기. 앱 최상단에 하나 두면 된다.
 * 사건이 여러 개 동시에 와도 줄을 세워 하나씩 보여준다 (클리어 → 완벽한 하루 → 레벨업 → 랭크업 …).
 * 새 연출을 추가하려면: core:game 의 Fx 에 사건을 추가하고, 아래 when 에 한 줄 넣는다.
 */
@Composable
fun FxHost(fx: Flow<Fx>, modifier: Modifier = Modifier, sounds: FxSounds? = null) {
    val queue = remember { mutableStateListOf<Fx>() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(fx) {
        fx.collect { queue.add(it) }
    }

    Box(modifier.fillMaxSize()) {
        val current = queue.firstOrNull() ?: return@Box
        val done: () -> Unit = { queue.remove(current) }

        LaunchedEffect(current) {
            haptic.performHapticFeedback(
                if (current is Fx.QuestClear) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress,
            )
            sounds?.play(current.sound())
        }

        key(current) {
            when (current) {
                is Fx.QuestClear -> QuestClearToast(current, done)
                is Fx.LevelUp -> LevelUpOverlay(current.record, done)
                is Fx.RankUp -> RankUpOverlay(current, done)
                is Fx.PerfectDay -> NoticeOverlay(
                    header = "DAILY QUEST CLEAR",
                    headline = "+${current.bonusXp} EXP",
                    lines = listOf("오늘의 퀘스트를 모두 완수했습니다."),
                    onDone = done,
                    burst = true,
                )
                is Fx.BossDefeated -> NoticeOverlay(
                    header = "WEEKLY BOSS",
                    headline = "${current.bossName} 처치",
                    lines = listOf("보상 EXP +${current.bonusXp}", "다음 보스는 월요일에 나타납니다."),
                    onDone = done,
                    burst = true,
                )
                is Fx.ClassChange -> NoticeOverlay(
                    header = "CLASS CHANGE",
                    headline = current.to.label,
                    lines = listOf("${current.from.label} → ${current.to.label}", "가장 많이 성장한 능력치가 바뀌었습니다."),
                    onDone = done,
                )
                is Fx.SeasonClear -> NoticeOverlay(
                    header = "SEASON ${current.record.season} CLEAR",
                    headline = "66일 완주",
                    lines = listOf(
                        "획득 EXP ${current.record.xpGained} · 도달 레벨 ${current.record.levelAtEnd}",
                        "완벽한 하루 ${current.record.perfectDays}일 · 연속 기준 달성 ${current.record.successDays}일",
                        "시즌 ${current.record.season + 1} 이 시작됩니다.",
                    ),
                    onDone = done,
                    burst = true,
                )
            }
        }
    }
}

/** 연출별 효과음. 소리 파일을 바꾸려면 FxSounds 설명 참고. */
private fun Fx.sound(): Sfx = when (this) {
    is Fx.QuestClear -> Sfx.QUEST_CLEAR
    is Fx.PerfectDay -> Sfx.PERFECT_DAY
    is Fx.LevelUp -> Sfx.LEVEL_UP
    is Fx.RankUp -> Sfx.RANK_UP
    is Fx.BossDefeated -> Sfx.BOSS
    is Fx.ClassChange, is Fx.SeasonClear -> Sfx.NOTICE
}
