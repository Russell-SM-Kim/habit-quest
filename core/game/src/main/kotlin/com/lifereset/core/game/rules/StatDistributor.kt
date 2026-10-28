package com.lifereset.core.game.rules

import com.lifereset.core.game.model.Stat
import kotlin.math.floor

/**
 * 레벨업 때 스탯을 자동으로 올리는 규칙 ('맞춤형 스탯').
 *
 * 1. 모든 스탯 +1
 * 2. 보너스 포인트를 지난 레벨 동안 쌓인 스탯 경험치 비율로 나눈다.
 *    소수점은 버리고, 남은 점수는 소수점이 큰 스탯부터 1점씩 (최대잔여법).
 * 3. 스탯 경험치가 하나도 없으면(보너스 EXP 만으로 레벨업) 낮은 스탯부터 채운다.
 */
object StatDistributor {

    fun distribute(
        bonusPoints: Int,
        statXp: Map<Stat, Double>,
        currentStats: Map<Stat, Int>,
    ): Map<Stat, Int> {
        val gains = Stat.entries.associateWith { 1 }.toMutableMap()
        val total = Stat.entries.sumOf { statXp[it] ?: 0.0 }

        if (total <= 0.0) {
            val lowestFirst = Stat.entries.sortedWith(
                compareBy<Stat> { currentStats[it] ?: 0 }.thenBy { it.ordinal },
            )
            repeat(bonusPoints) { i ->
                val stat = lowestFirst[i % lowestFirst.size]
                gains[stat] = gains.getValue(stat) + 1
            }
            return gains
        }

        val shares = Stat.entries.associateWith { bonusPoints * (statXp[it] ?: 0.0) / total }
        val floors = shares.mapValues { floor(it.value + EPSILON).toInt() }
        floors.forEach { (stat, points) -> gains[stat] = gains.getValue(stat) + points }

        var remaining = bonusPoints - floors.values.sum()
        val byRemainder = Stat.entries.sortedWith(
            compareByDescending<Stat> { shares.getValue(it) - floors.getValue(it) }
                .thenByDescending { statXp[it] ?: 0.0 }
                .thenBy { it.ordinal },
        )
        for (stat in byRemainder) {
            if (remaining <= 0) break
            gains[stat] = gains.getValue(stat) + 1
            remaining--
        }
        return gains
    }

    /** 3.0000000000000004 같은 부동소수점 오차가 순위를 뒤집지 않게. */
    private const val EPSILON = 1e-9
}
