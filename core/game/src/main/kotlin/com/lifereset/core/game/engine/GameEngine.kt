package com.lifereset.core.game.engine

import com.lifereset.core.game.model.BossState
import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.DaySummary
import com.lifereset.core.game.model.LevelUpRecord
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestSlot
import com.lifereset.core.game.model.SeasonRecord
import com.lifereset.core.game.model.Stat
import com.lifereset.core.game.model.TodayState
import com.lifereset.core.game.rules.GameRules
import com.lifereset.core.game.rules.PlayerClass
import com.lifereset.core.game.rules.Rank
import com.lifereset.core.game.rules.RecoveryQuest
import com.lifereset.core.game.rules.Seasons
import com.lifereset.core.game.rules.StatDistributor
import com.lifereset.core.game.rules.WeeklyBoss
import com.lifereset.core.game.rules.XpCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max

/** 엔진 입력: 시작일, 오늘, 퀘스트 정의, 완료 기록. 이것만 있으면 상태 전체를 다시 만들 수 있다. */
data class EngineInput(
    val gameStart: LocalDate,
    val today: LocalDate,
    val quests: List<QuestDef>,
    val completions: List<Completion>,
)

/**
 * 기록을 시작일부터 하루씩 다시 재생해서 [PlayerState] 를 만든다.
 * 같은 입력이면 언제나 같은 결과가 나온다 (시계, 랜덤을 쓰지 않는다).
 */
class GameEngine(private val rules: GameRules = GameRules.Default) {
    fun compute(input: EngineInput): PlayerState = Replay(rules, input).run()
}

private class Replay(
    private val rules: GameRules,
    private val input: EngineInput,
) {
    private val start: LocalDate = minOf(input.gameStart, input.today)

    private var level = 1
    private var xpInLevel = 0
    private var totalXp = 0L
    private val stats: MutableMap<Stat, Int> = Stat.entries.associateWith { rules.startingStat }.toMutableMap()
    private val statXp: MutableMap<Stat, Double> = Stat.entries.associateWith { 0.0 }.toMutableMap()

    private var streak = 0
    private var displayStreak = 0
    private var bestStreak = 0
    private var shields = 0
    /** 오늘 회복 퀘스트로 되살릴 수 있는 연속 일수. 어제 끊겼을 때만 값이 있다. */
    private var recoveryOffer: Int? = null

    private var boss: BossState? = null
    private var bossesDefeated = 0
    private var totalCompletions = 0

    private val history = mutableListOf<DaySummary>()
    private val levelUps = mutableListOf<LevelUpRecord>()
    private val seasonRecords = mutableListOf<SeasonRecord>()
    private var seasonStartXp = 0L
    private var seasonPerfectDays = 0
    private var seasonSuccessDays = 0

    private var todayState: TodayState? = null

    fun run(): PlayerState {
        val byDate = input.completions.filterNot { it.voided }.groupBy { it.date }
        var date = start
        while (!date.isAfter(input.today)) {
            processDay(date, byDate[date].orEmpty())
            date = date.plusDays(1)
        }
        return buildState()
    }

    private fun processDay(date: LocalDate, completions: List<Completion>) {
        val isToday = date == input.today
        val position = Seasons.position(start, date, rules)
        if (position.day == 1) {
            seasonStartXp = totalXp
            seasonPerfectDays = 0
            seasonSuccessDays = 0
        }
        if (date == start || date.dayOfWeek == DayOfWeek.MONDAY) spawnBoss(date)

        // 하루에 한 슬롯당 가장 먼저 완료한 1건만 인정한다 (택1 그룹 중복 방지)
        val counted = LinkedHashMap<String, Completion>()
        completions.sortedBy { it.completedAt }.forEach { counted.putIfAbsent(it.slotKey, it) }
        val recovery = counted.remove(RecoveryQuest.SLOT_KEY)

        // 회복 퀘스트: 어제 끊긴 연속 기록을 되살린다
        val offer = recoveryOffer
        recoveryOffer = null
        // 회복 기회가 없는 날의 회복 완료 기록은 무시한다
        val validRecovery: Completion? = if (offer != null) recovery else null
        val recovered = validRecovery != null
        if (offer != null && validRecovery != null) streak = offer

        val streakBefore = streak
        val scored = (counted.values + listOfNotNull(validRecovery)).sortedBy { it.completedAt }
        scored.forEach { score(it, streakBefore, date) }

        val dueSlots: Map<String, List<QuestDef>> = input.quests
            .filter { it.isDueOn(date) }
            .sortedBy { it.sortOrder }
            .groupBy { it.slotKey }
        val doneCount = dueSlots.keys.count { it in counted }
        val perfect = dueSlots.isNotEmpty() && doneCount == dueSlots.size
        if (perfect) gainXp(rules.perfectDayBonusXp, date)
        val needed = ceil(dueSlots.size * rules.streakSuccessRatio - 1e-9).toInt()
        val success = dueSlots.isEmpty() || doneCount >= needed

        var shieldUsed = false
        if (isToday) {
            // 오늘은 아직 끝나지 않았으므로 연속 기록을 확정하지 않는다
            displayStreak = if (dueSlots.isNotEmpty() && success) streak + 1 else streak
            todayState = TodayState(
                date = date,
                slots = dueSlots.map { (key, quests) -> QuestSlot(key, quests, counted[key]) },
                recoveryAvailable = offer != null && !recovered,
                recoveryCompletion = validRecovery,
                perfect = perfect,
            )
        } else {
            when {
                dueSlots.isEmpty() -> Unit // 휴식일: 끊지도 늘리지도 않는다
                success -> {
                    streak++
                    if (streak % rules.shieldEveryDays == 0 && shields < rules.maxShields) shields++
                }
                shields > 0 -> {
                    shields--
                    shieldUsed = true
                }
                else -> {
                    if (streak > 0) recoveryOffer = streak
                    streak = 0
                }
            }
            if (dueSlots.isNotEmpty() && success) seasonSuccessDays++
            if (perfect) seasonPerfectDays++
            if (position.day == rules.seasonLength) closeSeason(position.season, position.seasonStart, date)
        }

        bestStreak = max(bestStreak, if (isToday) displayStreak else streak)
        history += DaySummary(date, dueSlots.size, doneCount, perfect, success, shieldUsed)
    }

    private fun score(completion: Completion, streakBefore: Int, date: LocalDate) {
        val xp = XpCalculator.forCompletion(completion, streakBefore, rules)
        // 스탯 경험치를 먼저 쌓아야 이번 완료로 레벨업할 때 분배에 반영된다
        XpCalculator.statXp(completion, xp, rules).forEach { (stat, value) ->
            statXp[stat] = statXp.getValue(stat) + value
        }
        totalCompletions++
        gainXp(xp.total, date)
        if (completion.category == QuestCategory.EXERCISE) damageBoss(date)
    }

    private fun gainXp(amount: Int, date: LocalDate) {
        totalXp += amount
        xpInLevel += amount
        while (xpInLevel >= rules.xpToNext(level)) {
            xpInLevel -= rules.xpToNext(level)
            levelUp(date)
        }
    }

    private fun levelUp(date: LocalDate) {
        val rankBefore = Rank.of(level)
        val classBefore = PlayerClass.classify(stats, level, rules)
        level++
        val rankAfter = Rank.of(level)
        val gains = StatDistributor.distribute(rankAfter.bonusPoints, statXp, stats)
        gains.forEach { (stat, gain) -> stats[stat] = stats.getValue(stat) + gain }
        Stat.entries.forEach { statXp[it] = 0.0 }
        levelUps += LevelUpRecord(
            date = date,
            newLevel = level,
            gains = gains,
            rankBefore = rankBefore,
            rankAfter = rankAfter,
            classBefore = classBefore,
            classAfter = PlayerClass.classify(stats, level, rules),
        )
    }

    private fun spawnBoss(date: LocalDate) {
        val weekStart = WeeklyBoss.weekStartOf(date)
        boss = WeeklyBoss.spawn(weekStart, start, input.quests)?.let {
            BossState(name = it.name, weekStart = it.weekStart, maxHp = it.maxHp, damage = 0, defeated = false)
        }
    }

    private fun damageBoss(date: LocalDate) {
        val current = boss ?: return
        if (current.defeated) return
        val hit = current.copy(damage = current.damage + 1)
        if (hit.damage >= hit.maxHp) {
            boss = hit.copy(defeated = true)
            bossesDefeated++
            gainXp(rules.bossBonusXp, date)
        } else {
            boss = hit
        }
    }

    private fun closeSeason(season: Int, seasonStart: LocalDate, end: LocalDate) {
        seasonRecords += SeasonRecord(
            season = season,
            start = seasonStart,
            end = end,
            xpGained = totalXp - seasonStartXp,
            levelAtEnd = level,
            perfectDays = seasonPerfectDays,
            successDays = seasonSuccessDays,
        )
    }

    private fun buildState(): PlayerState {
        val statXpTotal = statXp.values.sum()
        val growthShare = Stat.entries.associateWith {
            if (statXpTotal > 0) statXp.getValue(it) / statXpTotal else 0.0
        }
        return PlayerState(
            level = level,
            xpInLevel = xpInLevel,
            xpToNext = rules.xpToNext(level),
            totalXp = totalXp,
            rank = Rank.of(level),
            rankLabel = Rank.label(level),
            stats = stats.toMap(),
            growthShare = growthShare,
            playerClass = PlayerClass.classify(stats, level, rules),
            streak = displayStreak,
            bestStreak = bestStreak,
            shields = shields,
            season = Seasons.position(start, input.today, rules),
            today = checkNotNull(todayState) { "오늘 상태가 계산되지 않았습니다" },
            boss = boss,
            history = history.toList(),
            levelUps = levelUps.toList(),
            seasonRecords = seasonRecords.toList(),
            bossesDefeated = bossesDefeated,
            totalCompletions = totalCompletions,
        )
    }
}
