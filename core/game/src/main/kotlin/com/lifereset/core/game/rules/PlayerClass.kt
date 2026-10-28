package com.lifereset.core.game.rules

import com.lifereset.core.game.model.Stat

/**
 * 클래스(직업). 레벨업마다 다시 판정한다.
 *
 * 1. 레벨이 [GameRules.noviceUntilLevel] 미만이면 견습생 (아직 방향이 정해지지 않은 초보).
 * 2. 모든 능력치가 [GameRules.allRounderMinStat] 이상이고 고르게 높으면 올라운더.
 * 3. 그 외에는 가장 높은 능력치로 정한다.
 */
enum class PlayerClass(val label: String, val primary: Stat?) {
    NOVICE("견습생", null),
    WARRIOR("전사", Stat.STR),
    RUNNER("러너", Stat.END),
    STRIKER("스트라이커", Stat.AGI),
    SAGE("현자", Stat.INT),
    MONK("수도승", Stat.WIL),
    GUARDIAN("수호자", Stat.VIT),
    ALL_ROUNDER("올라운더", null),
    ;

    companion object {
        fun classify(stats: Map<Stat, Int>, level: Int, rules: GameRules = GameRules.Default): PlayerClass {
            if (level < rules.noviceUntilLevel) return NOVICE
            val values = Stat.entries.map { stats[it] ?: 0 }
            val max = values.max()
            val min = values.min()
            val balanced = max - min <= max * rules.allRounderSpread
            if (min >= rules.allRounderMinStat && balanced) return ALL_ROUNDER
            // 동점이면 Stat 선언 순서가 앞인 쪽
            val top = Stat.entries.maxBy { stats[it] ?: 0 }
            return entries.first { it.primary == top }
        }
    }
}
