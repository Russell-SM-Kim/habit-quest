package com.lifereset.core.game

import com.lifereset.core.game.engine.EngineInput
import com.lifereset.core.game.engine.Fx
import com.lifereset.core.game.engine.FxDiff
import com.lifereset.core.game.engine.GameEngine
import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.rules.GameRules
import com.lifereset.core.game.rules.PlayerClass
import com.lifereset.core.game.rules.RecoveryQuest
import com.lifereset.core.game.seed.DefaultQuests
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameEngineTest {
    private val engine = GameEngine()
    private val start: LocalDate = LocalDate.of(2026, 10, 5) // 월요일
    private val quests: List<QuestDef> = DefaultQuests.create(start)

    private fun millis(date: LocalDate, hour: Int = 9): Long =
        date.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    /** 그날 예정된 슬롯을 전부 (택1 은 첫 번째 퀘스트로) 완료한다. */
    private fun completeAll(date: LocalDate): List<Completion> =
        quests.filter { it.isDueOn(date) }
            .groupBy { it.slotKey }
            .values
            .mapIndexed { i, slot -> Completion.of(slot.first(), date, millis(date) + i) }

    private fun completeDays(from: LocalDate, days: Int): List<Completion> =
        (0 until days).flatMap { completeAll(from.plusDays(it.toLong())) }

    private fun run(today: LocalDate, completions: List<Completion>) =
        engine.compute(EngineInput(start, today, quests, completions))

    @Test
    fun `첫날 아무것도 안 하면 레벨 1, 스탯 10, 견습생`() {
        val state = run(start, emptyList())
        assertEquals(1, state.level)
        assertTrue(state.stats.values.all { it == 10 })
        assertEquals(PlayerClass.NOVICE, state.playerClass)
        assertEquals(1, state.season.season)
        assertEquals(1, state.season.day)
        assertEquals(0, state.streak)
    }

    @Test
    fun `7일 연속 완벽하면 연속 7일과 보호권 1장`() {
        val today = start.plusDays(7)
        val state = run(today, completeDays(start, 7))
        assertEquals(7, state.streak)
        assertEquals(1, state.shields)
        assertTrue(state.level > 1)
    }

    @Test
    fun `오늘 기준을 넘기면 연속 일수에 오늘이 포함된다`() {
        val today = start.plusDays(3)
        val state = run(today, completeDays(start, 4))
        assertEquals(4, state.streak)
        assertTrue(state.today.perfect)
    }

    @Test
    fun `하루를 놓치면 끊기고 다음날 회복 퀘스트로 되살린다`() {
        val missed = start.plusDays(3)
        val nextDay = missed.plusDays(1)
        val history = completeDays(start, 3)

        val broken = run(nextDay, history)
        assertEquals(0, broken.streak)
        assertTrue(broken.today.recoveryAvailable)

        val recovery = Completion.of(RecoveryQuest.definition(nextDay), nextDay, millis(nextDay))
        val restored = run(nextDay, history + recovery)
        assertEquals(3, restored.streak)
        assertFalse(restored.today.recoveryAvailable)
        assertNotNull(restored.today.recoveryCompletion)
    }

    @Test
    fun `회복 기회는 다음날 하루뿐이다`() {
        val twoDaysLater = start.plusDays(5)
        val state = run(twoDaysLater, completeDays(start, 3))
        assertFalse(state.today.recoveryAvailable)
    }

    @Test
    fun `택1 그룹은 둘 다 해도 한 번만 인정된다`() {
        val tuesday = start.plusDays(1)
        val tennis = quests.first { it.id == "q-table-tennis" }
        val running = quests.first { it.id == "q-running" }
        val one = run(tuesday, listOf(Completion.of(tennis, tuesday, millis(tuesday))))
        val both = run(
            tuesday,
            listOf(
                Completion.of(tennis, tuesday, millis(tuesday)),
                Completion.of(running, tuesday, millis(tuesday) + 1),
            ),
        )
        assertEquals(one.totalXp, both.totalXp)
        assertEquals(1, both.today.slots.count { it.done })
    }

    @Test
    fun `레벨업하면 모든 스탯 +1 과 랭크 보너스만큼 오른다`() {
        val state = run(start.plusDays(10), completeDays(start, 10))
        val levelUp = state.levelUps.first()
        assertEquals(6 + 5, levelUp.gains.values.sum()) // 기본 6 + E랭크 보너스 5
        assertTrue(levelUp.gains.values.all { it >= 1 })
    }

    @Test
    fun `많이 한 활동의 스탯이 더 오른다`() {
        // 독서만 계속하면 INT 가 가장 높아진다
        val reading = quests.first { it.id == "q-reading" }
        val completions = (0L until 20L).map { day ->
            val date = start.plusDays(day)
            Completion.of(reading, date, millis(date), amount = 40.0)
        }
        val state = run(start.plusDays(20), completions)
        val top = state.stats.maxBy { it.value }.key
        assertEquals(Stat.INT, top)
        assertEquals(PlayerClass.SAGE, state.playerClass)
    }

    @Test
    fun `66일이 지나면 시즌 1 기록이 남고 시즌 2가 시작된다`() {
        val state = run(start.plusDays(66), completeDays(start, 66))
        assertEquals(1, state.seasonRecords.size)
        assertEquals(2, state.season.season)
        assertEquals(1, state.season.day)
        assertEquals(67, state.season.totalDay)
    }

    @Test
    fun `운동을 충분히 하면 주간 보스를 잡는다`() {
        val sunday = start.plusDays(6)
        val state = run(sunday, completeDays(start, 7))
        val boss = assertNotNull(state.boss)
        assertTrue(boss.defeated)
        assertEquals(1, state.bossesDefeated)
    }

    @Test
    fun `취소한 기록은 계산에서 빠진다`() {
        val c = completeAll(start).first()
        val state = run(start, listOf(c.copy(voided = true)))
        assertEquals(0L, state.totalXp)
    }

    @Test
    fun `완료로 레벨업하면 연출 목록에 레벨업이 들어간다`() {
        val today = start.plusDays(2)
        val before = completeDays(start, 2)
        val old = run(today, before)
        var history = before
        var new = old
        for (c in completeAll(today)) {
            history = history + c
            new = run(today, history)
            if (new.level > old.level) break
        }
        val fx = FxDiff.between(old, new, GameRules.Default.perfectDayBonusXp, GameRules.Default.bossBonusXp)
        assertTrue(fx.any { it is Fx.LevelUp })
    }
}
