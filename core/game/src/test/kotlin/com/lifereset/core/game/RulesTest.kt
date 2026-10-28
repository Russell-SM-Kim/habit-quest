package com.lifereset.core.game

import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestTarget
import com.lifereset.core.game.model.QuestUnit
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.model.StatWeightsCodec
import com.lifereset.core.game.rules.PlayerClass
import com.lifereset.core.game.rules.Rank
import com.lifereset.core.game.rules.StatDistributor
import com.lifereset.core.game.rules.StrengthProgram
import com.lifereset.core.game.rules.XpCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RulesTest {
    private val day = LocalDate.of(2026, 10, 5)

    private val reading = QuestDef(
        id = "q-reading", title = "독서", difficulty = Difficulty.NORMAL,
        weights = mapOf(Stat.INT to 1.0), days = DayOfWeek.entries.toSet(), createdOn = day,
        target = QuestTarget(20.0, QuestUnit.MINUTES),
    )

    @Test
    fun `보너스 포인트는 스탯 경험치 비율로 나뉜다`() {
        val gains = StatDistributor.distribute(
            bonusPoints = 5,
            statXp = mapOf(Stat.END to 60.0, Stat.AGI to 30.0, Stat.INT to 10.0),
            currentStats = Stat.entries.associateWith { 10 },
        )
        // 기본 +1 포함: END 1+3, AGI 1+2, INT 1+0
        assertEquals(4, gains[Stat.END])
        assertEquals(3, gains[Stat.AGI])
        assertEquals(1, gains[Stat.INT])
        assertEquals(1, gains[Stat.STR])
        assertEquals(11, gains.values.sum())
    }

    @Test
    fun `스탯 경험치가 없으면 낮은 스탯부터 채운다`() {
        val current = Stat.entries.associateWith { 10 } + (Stat.VIT to 8)
        val gains = StatDistributor.distribute(1, emptyMap(), current)
        assertEquals(2, gains[Stat.VIT])
    }

    @Test
    fun `독서 20분 목표에 50분 하면 상한 2배까지 받는다`() {
        val c = Completion.of(reading, day, 0L, amount = 50.0)
        val xp = XpCalculator.forCompletion(c, streakBefore = 0)
        assertEquals(20, xp.base)
        assertEquals(20, xp.overachieve)
        assertEquals(40, xp.total)
    }

    @Test
    fun `30분이면 초과 50퍼센트만큼 받는다`() {
        val c = Completion.of(reading, day, 0L, amount = 30.0)
        assertEquals(30, XpCalculator.forCompletion(c, streakBefore = 0).total)
    }

    @Test
    fun `연속 10일이면 10퍼센트 보너스`() {
        val c = Completion.of(reading, day, 0L, amount = 50.0)
        assertEquals(44, XpCalculator.forCompletion(c, streakBefore = 10).total)
    }

    @Test
    fun `목표를 못 채우면 초과 보너스는 0`() {
        val c = Completion.of(reading, day, 0L, amount = 15.0)
        assertEquals(20, XpCalculator.forCompletion(c, streakBefore = 0).total)
    }

    @Test
    fun `랭크 경계와 S 랭크 별`() {
        assertEquals(Rank.E, Rank.of(9))
        assertEquals(Rank.D, Rank.of(10))
        assertEquals(Rank.S, Rank.of(70))
        assertEquals("S", Rank.label(79))
        assertEquals("S★1", Rank.label(80))
    }

    @Test
    fun `근력 단계는 주차에 따라 오른다`() {
        assertEquals(1, StrengthProgram.phaseForWeek(1).number)
        assertEquals(2, StrengthProgram.phaseForWeek(3).number)
        assertEquals(4, StrengthProgram.phaseForWeek(10).number)
        assertEquals(5, StrengthProgram.phaseForWeek(11).number)
        assertEquals(6, StrengthProgram.phaseForWeek(14).number)
    }

    @Test
    fun `레벨 5 미만은 견습생, 올라운더는 모든 스탯 30 이상일 때만`() {
        val even = Stat.entries.associateWith { 20 }
        assertEquals(PlayerClass.NOVICE, PlayerClass.classify(even, level = 4))
        // 고르지만 30 미만이면 올라운더가 아니라 가장 높은(동점이면 첫) 스탯 클래스
        assertEquals(PlayerClass.WARRIOR, PlayerClass.classify(even, level = 10))
        val strong = Stat.entries.associateWith { 32 } + (Stat.END to 35)
        assertEquals(PlayerClass.ALL_ROUNDER, PlayerClass.classify(strong, level = 30))
        val runner = Stat.entries.associateWith { 32 } + (Stat.END to 60)
        assertEquals(PlayerClass.RUNNER, PlayerClass.classify(runner, level = 30))
    }

    @Test
    fun `스탯 가중치 문자열 변환`() {
        val weights = mapOf(Stat.VIT to 0.2, Stat.END to 0.8)
        val text = StatWeightsCodec.encode(weights)
        assertEquals("END:0.8,VIT:0.2", text)
        assertEquals(weights, StatWeightsCodec.decode(text))
    }
}
