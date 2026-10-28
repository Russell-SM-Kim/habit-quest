package com.lifereset.core.game.engine

import com.lifereset.core.game.model.LevelUpRecord
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.SeasonRecord
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.rules.PlayerClass
import com.lifereset.core.game.rules.Rank

/**
 * 화면 연출로 보여줄 사건. UI 와 무관한 '무슨 일이 일어났나'만 담는다.
 * 어떻게 보여줄지는 core:designsystem 의 연출 컴포넌트가 정한다.
 */
sealed interface Fx {
    /** 퀘스트 하나를 끝냈다. xp 는 이 행동으로 늘어난 총 EXP (보너스 포함). */
    data class QuestClear(val title: String, val xp: Int, val stats: List<Stat>) : Fx
    data class PerfectDay(val bonusXp: Int) : Fx
    data class BossDefeated(val bossName: String, val bonusXp: Int) : Fx
    data class LevelUp(val record: LevelUpRecord) : Fx
    data class RankUp(val from: Rank, val to: Rank, val label: String) : Fx
    data class ClassChange(val from: PlayerClass, val to: PlayerClass) : Fx
    data class SeasonClear(val record: SeasonRecord) : Fx
}

/** 두 상태를 비교해서 새로 생긴 사건을 재생 순서대로 돌려준다. */
object FxDiff {

    fun between(old: PlayerState, new: PlayerState, perfectBonus: Int, bossBonus: Int): List<Fx> = buildList {
        val sameDay = old.today.date == new.today.date
        if (sameDay && !old.today.perfect && new.today.perfect) add(Fx.PerfectDay(perfectBonus))

        val oldBoss = old.boss
        val newBoss = new.boss
        if (newBoss != null && newBoss.defeated &&
            (oldBoss == null || oldBoss.weekStart != newBoss.weekStart || !oldBoss.defeated)
        ) {
            add(Fx.BossDefeated(newBoss.name, bossBonus))
        }

        val freshLevelUps = new.levelUps.drop(old.levelUps.size)
        freshLevelUps.forEach { add(Fx.LevelUp(it)) }
        freshLevelUps.firstOrNull()?.let { first ->
            val last = freshLevelUps.last()
            if (first.rankBefore != last.rankAfter) {
                add(Fx.RankUp(first.rankBefore, last.rankAfter, new.rankLabel))
            }
            if (first.classBefore != last.classAfter) {
                add(Fx.ClassChange(first.classBefore, last.classAfter))
            }
        }

        new.seasonRecords.drop(old.seasonRecords.size).forEach { add(Fx.SeasonClear(it)) }
    }
}
