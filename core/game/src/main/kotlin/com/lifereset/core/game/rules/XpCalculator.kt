package com.lifereset.core.game.rules

import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.Stat
import kotlin.math.min
import kotlin.math.roundToInt

/** 완료 1건이 주는 EXP 의 내역. 화면에 '+25 (기본 20 · 초과 5)' 처럼 보여줄 수 있다. */
data class XpBreakdown(
    val base: Int,
    val overachieve: Int,
    val streak: Int,
    /** 스탯 경험치 계산에 쓰는 값 (기본 + 초과, 연속 보너스는 제외). */
    val statXpPool: Double,
) {
    val total: Int get() = base + overachieve + streak
}

object XpCalculator {

    /**
     * @param streakBefore 그날 시작 시점의 연속 일수
     */
    fun forCompletion(
        completion: Completion,
        streakBefore: Int,
        rules: GameRules = GameRules.Default,
    ): XpBreakdown {
        val base = completion.difficulty.baseXp
        val overRatio = overachieveRatio(completion, rules)
        val overXp = (base * overRatio).roundToInt()
        val streakRate = min(streakBefore * rules.streakBonusPerDay, rules.streakBonusCap)
        val streakXp = ((base + overXp) * streakRate).roundToInt()
        return XpBreakdown(
            base = base,
            overachieve = overXp,
            streak = streakXp,
            statXpPool = (base + overXp).toDouble(),
        )
    }

    /** 목표 대비 초과 비율 (0 ~ overachieveCap). 목표가 없거나 못 넘겼으면 0. */
    fun overachieveRatio(completion: Completion, rules: GameRules = GameRules.Default): Double {
        val target = completion.targetAmount ?: return 0.0
        val amount = completion.amount ?: return 0.0
        if (target <= 0.0 || amount <= target) return 0.0
        return min((amount - target) / target, rules.overachieveCap)
    }

    /** 완료 1건이 각 스탯에 주는 스탯 경험치. */
    fun statXp(
        completion: Completion,
        breakdown: XpBreakdown,
        rules: GameRules = GameRules.Default,
    ): Map<Stat, Double> = completion.weights.mapValues { (stat, weight) ->
        val winBonus = if (completion.won && stat == rules.winStat) rules.winStatMultiplier else 1.0
        breakdown.statXpPool * weight * winBonus
    }
}
