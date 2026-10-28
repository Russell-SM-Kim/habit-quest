package com.lifereset.core.data.db

import com.lifereset.core.game.model.Completion
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.protocol.sync.CompletionDto
import com.lifereset.core.protocol.sync.QuestDto
import com.lifereset.core.protocol.sync.toDomain
import com.lifereset.core.protocol.sync.toDto

/*
 * Entity <-> 전송 형식(DTO) <-> 도메인.
 * Entity 와 DTO 는 필드가 같으므로, 도메인 변환은 core:protocol 의 매퍼를 그대로 재사용한다.
 */

internal fun QuestEntity.toDto() = QuestDto(
    id = id,
    title = title,
    difficulty = difficulty,
    weights = weights,
    days = days.split(",").mapNotNull { it.trim().toIntOrNull() },
    createdOn = createdOn,
    targetAmount = targetAmount,
    targetUnit = targetUnit,
    note = note,
    category = category,
    choiceGroup = choiceGroup,
    tracksWin = tracksWin,
    programId = programId,
    archivedOn = archivedOn,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
)

internal fun QuestDto.toEntity(dirty: Boolean) = QuestEntity(
    id = id,
    title = title,
    difficulty = difficulty,
    weights = weights,
    days = days.joinToString(","),
    createdOn = createdOn,
    targetAmount = targetAmount,
    targetUnit = targetUnit,
    note = note,
    category = category,
    choiceGroup = choiceGroup,
    tracksWin = tracksWin,
    programId = programId,
    archivedOn = archivedOn,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    dirty = dirty,
)

internal fun QuestEntity.toDomain(): QuestDef = toDto().toDomain()
internal fun QuestDef.toEntity(updatedAt: Long, dirty: Boolean) = toDto(updatedAt).toEntity(dirty)

internal fun CompletionEntity.toDto() = CompletionDto(
    id = id,
    questId = questId,
    questTitle = questTitle,
    date = date,
    completedAt = completedAt,
    updatedAt = updatedAt,
    slotKey = slotKey,
    difficulty = difficulty,
    weights = weights,
    category = category,
    targetAmount = targetAmount,
    amount = amount,
    won = won,
    voided = voided,
)

internal fun CompletionDto.toEntity(dirty: Boolean) = CompletionEntity(
    id = id,
    questId = questId,
    questTitle = questTitle,
    date = date,
    completedAt = completedAt,
    updatedAt = updatedAt,
    slotKey = slotKey,
    difficulty = difficulty,
    weights = weights,
    category = category,
    targetAmount = targetAmount,
    amount = amount,
    won = won,
    voided = voided,
    dirty = dirty,
)

internal fun CompletionEntity.toDomain(): Completion = toDto().toDomain()
internal fun Completion.toEntity(dirty: Boolean) = toDto().toEntity(dirty)
