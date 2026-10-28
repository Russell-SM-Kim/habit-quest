package com.lifereset.core.game.rules

import com.lifereset.core.game.model.Stat

/**
 * 밸런스 숫자는 전부 여기 모아 둔다. 게임 감각을 조정할 때는 이 파일만 고친다.
 * 기록은 원본 그대로 남아 있으므로, 숫자를 바꾸면 과거 기록 전체가 새 규칙으로 다시 계산된다.
 */
data class GameRules(
    /** 시즌 길이(일). 끝나면 다음 시즌이 자동으로 시작된다. */
    val seasonLength: Int = 66,
    /** 레벨 1 의 모든 스탯 값. */
    val startingStat: Int = 10,

    /** 레벨 L 에서 다음 레벨까지 필요한 EXP = xpBase + xpPerLevel × L */
    val xpBase: Int = 60,
    val xpPerLevel: Int = 40,

    /** 그날 예정된 퀘스트를 전부 끝내면 주는 보너스. */
    val perfectDayBonusXp: Int = 30,
    /** 주간 보스 처치 보상. */
    val bossBonusXp: Int = 60,

    /** 연속 일수 1일당 EXP 보너스 비율과 상한. */
    val streakBonusPerDay: Double = 0.01,
    val streakBonusCap: Double = 0.50,

    /** 목표 초과 달성 보너스 상한 (1.0 = 최대 +100%, 즉 2배). */
    val overachieveCap: Double = 1.0,

    /** 승패를 기록하는 퀘스트에서 이겼을 때 스탯 경험치 배율. */
    val winStat: Stat = Stat.AGI,
    val winStatMultiplier: Double = 1.5,

    /** 그날 예정 퀘스트의 이 비율 이상을 끝내면 연속 기록이 이어진다. */
    val streakSuccessRatio: Double = 0.8,
    /** 연속 N일마다 보호권 1장. */
    val shieldEveryDays: Int = 7,
    val maxShields: Int = 2,

    /** 이 레벨 미만은 클래스 없이 '견습생'. */
    val noviceUntilLevel: Int = 5,
    /** 올라운더 조건: 모든 스탯이 이 값 이상이고, */
    val allRounderMinStat: Int = 30,
    /** 최고·최저 스탯 차이가 최고값의 이 비율 이내. */
    val allRounderSpread: Double = 0.15,
) {
    fun xpToNext(level: Int): Int = xpBase + xpPerLevel * level

    companion object {
        val Default = GameRules()
    }
}
