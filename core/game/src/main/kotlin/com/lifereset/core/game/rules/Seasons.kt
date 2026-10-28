package com.lifereset.core.game.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 게임 시작일로부터 계산한 오늘의 위치. 시즌은 끝없이 이어진다. */
data class SeasonPosition(
    val season: Int,
    /** 시즌 안에서 몇 번째 날인지 (1 ~ seasonLength) */
    val day: Int,
    /** 게임 시작 후 누적 일차 (1부터) */
    val totalDay: Int,
    val seasonStart: LocalDate,
    val seasonLength: Int,
) {
    val daysLeft: Int get() = seasonLength - day
    val progress: Float get() = day.toFloat() / seasonLength
    val weekOfGame: Int get() = (totalDay - 1) / 7 + 1
}

object Seasons {
    fun position(gameStart: LocalDate, date: LocalDate, rules: GameRules = GameRules.Default): SeasonPosition {
        val index = ChronoUnit.DAYS.between(gameStart, date).toInt().coerceAtLeast(0)
        val seasonIndex = index / rules.seasonLength
        return SeasonPosition(
            season = seasonIndex + 1,
            day = index % rules.seasonLength + 1,
            totalDay = index + 1,
            seasonStart = gameStart.plusDays((seasonIndex * rules.seasonLength).toLong()),
            seasonLength = rules.seasonLength,
        )
    }
}
