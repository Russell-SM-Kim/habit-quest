package com.lifereset.core.protocol.sync

import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.Difficulty
import com.lifereset.core.game.model.PlayerState
import com.lifereset.core.game.model.QuestCategory
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.model.QuestTarget
import com.lifereset.core.game.model.QuestUnit
import com.lifereset.core.game.model.StatWeightsCodec
import java.time.DayOfWeek
import java.time.LocalDate

/* 도메인 모델 <-> 전송 형식. 알 수 없는 enum 값이 와도 앱이 죽지 않도록 기본값으로 떨어뜨린다. */

fun QuestDef.toDto(updatedAt: Long) = QuestDto(
    id = id,
    title = title,
    difficulty = difficulty.name,
    weights = StatWeightsCodec.encode(weights),
    days = days.map { it.value }.sorted(),
    createdOn = createdOn.toString(),
    targetAmount = target?.amount,
    targetUnit = target?.unit?.name,
    note = note,
    category = category.name,
    choiceGroup = choiceGroup,
    tracksWin = tracksWin,
    programId = programId,
    archivedOn = archivedOn?.toString(),
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)

fun QuestDto.toDomain() = QuestDef(
    id = id,
    title = title,
    difficulty = enumOr(difficulty, Difficulty.NORMAL),
    weights = StatWeightsCodec.decode(weights),
    days = days.mapNotNull { v -> DayOfWeek.entries.firstOrNull { it.value == v } }.toSet(),
    createdOn = LocalDate.parse(createdOn),
    target = targetFrom(targetAmount, targetUnit),
    note = note,
    category = enumOr(category, QuestCategory.HABIT),
    choiceGroup = choiceGroup,
    tracksWin = tracksWin,
    programId = programId,
    archivedOn = archivedOn?.let(LocalDate::parse),
    sortOrder = sortOrder,
)

fun Completion.toDto() = CompletionDto(
    id = id,
    questId = questId,
    questTitle = questTitle,
    date = date.toString(),
    completedAt = completedAt,
    updatedAt = updatedAt,
    slotKey = slotKey,
    difficulty = difficulty.name,
    weights = StatWeightsCodec.encode(weights),
    category = category.name,
    targetAmount = targetAmount,
    amount = amount,
    won = won,
    voided = voided,
)

fun CompletionDto.toDomain() = Completion(
    id = id,
    questId = questId,
    questTitle = questTitle,
    date = LocalDate.parse(date),
    completedAt = completedAt,
    updatedAt = updatedAt,
    slotKey = slotKey,
    difficulty = enumOr(difficulty, Difficulty.NORMAL),
    weights = StatWeightsCodec.decode(weights),
    category = enumOr(category, QuestCategory.HABIT),
    targetAmount = targetAmount,
    amount = amount,
    won = won,
    voided = voided,
)

fun PlayerState.toProfileDto() = ProfileDto(
    level = level,
    rank = rankLabel,
    playerClass = playerClass.label,
    totalXp = totalXp,
    streak = streak,
    bestStreak = bestStreak,
    season = season.season,
    seasonDay = season.day,
    stats = stats.mapKeys { it.key.code },
)

fun targetFrom(amount: Double?, unit: String?): QuestTarget? {
    if (amount == null || amount <= 0.0 || unit == null) return null
    val parsed = QuestUnit.entries.firstOrNull { it.name == unit } ?: return null
    return QuestTarget(amount, parsed)
}

inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: fallback
