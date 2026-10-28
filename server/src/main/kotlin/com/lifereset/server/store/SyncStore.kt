package com.lifereset.server.store

import com.lifereset.core.protocol.sync.CompletionDto
import com.lifereset.core.protocol.sync.PullResponse
import com.lifereset.core.protocol.sync.PushRequest
import com.lifereset.core.protocol.sync.PushResponse
import com.lifereset.core.protocol.sync.QuestDto
import com.lifereset.core.protocol.sync.SettingsDto
import com.lifereset.server.db.CompletionsTable
import com.lifereset.server.db.QuestsTable
import com.lifereset.server.db.SettingsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * 동기화 저장소. 모든 읽기·쓰기는 로그인한 사용자(userId)의 행만 다룬다.
 * 규칙은 하나: 같은 id 면 updatedAt 이 큰 쪽이 이긴다.
 * 같은 요청을 두 번 받아도 결과가 같다 (재시도 안전).
 */
class SyncStore(
    private val database: Database,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun push(userId: String, request: PushRequest): PushResponse = transaction(database) {
        val now = clock()
        val quests = request.quests.count { upsertQuest(userId, it, now) }
        val completions = request.completions.count { upsertCompletion(userId, it, now) }
        request.settings?.let { upsertSetting(userId, GAME_START_KEY, it.gameStart, it.updatedAt, now) }
        PushResponse(acceptedQuests = quests, acceptedCompletions = completions, serverTime = now)
    }

    /** since 이후(같은 시각 포함)에 서버가 받은 행. 같은 행을 한 번 더 받아도 updatedAt 비교로 무해하다. */
    fun pull(userId: String, since: Long): PullResponse = transaction(database) {
        val now = clock()
        PullResponse(
            quests = QuestsTable.selectAll()
                .where { (QuestsTable.userId eq userId) and (QuestsTable.serverUpdatedAt greaterEq since) }
                .map { it.toQuestDto() },
            completions = CompletionsTable.selectAll()
                .where { (CompletionsTable.userId eq userId) and (CompletionsTable.serverUpdatedAt greaterEq since) }
                .map { it.toCompletionDto() },
            settings = readSettings(userId),
            serverTime = now,
        )
    }

    /** 엔진 계산용 전체 데이터. */
    fun snapshot(userId: String): Triple<List<QuestDto>, List<CompletionDto>, SettingsDto?> = transaction(database) {
        Triple(
            QuestsTable.selectAll().where { QuestsTable.userId eq userId }.map { it.toQuestDto() },
            CompletionsTable.selectAll().where { CompletionsTable.userId eq userId }.map { it.toCompletionDto() },
            readSettings(userId),
        )
    }

    // --- 쓰기 -------------------------------------------------------------

    private fun upsertQuest(userId: String, dto: QuestDto, now: Long): Boolean {
        val match = { (QuestsTable.userId eq userId) and (QuestsTable.id eq dto.id) }
        val existing = QuestsTable.selectAll().where { match() }
            .singleOrNull()?.get(QuestsTable.updatedAt)
        return when {
            existing == null -> {
                QuestsTable.insert { it.fillQuest(userId, dto, now) }
                true
            }
            dto.updatedAt > existing -> {
                QuestsTable.update({ match() }) { it.fillQuest(userId, dto, now) }
                true
            }
            else -> false
        }
    }

    private fun upsertCompletion(userId: String, dto: CompletionDto, now: Long): Boolean {
        val match = { (CompletionsTable.userId eq userId) and (CompletionsTable.id eq dto.id) }
        val existing = CompletionsTable.selectAll().where { match() }
            .singleOrNull()?.get(CompletionsTable.updatedAt)
        return when {
            existing == null -> {
                CompletionsTable.insert { it.fillCompletion(userId, dto, now) }
                true
            }
            dto.updatedAt > existing -> {
                CompletionsTable.update({ match() }) { it.fillCompletion(userId, dto, now) }
                true
            }
            else -> false
        }
    }

    private fun upsertSetting(userId: String, key: String, value: String, updatedAt: Long, now: Long) {
        val match = { (SettingsTable.userId eq userId) and (SettingsTable.key eq key) }
        val existing = SettingsTable.selectAll().where { match() }
            .singleOrNull()?.get(SettingsTable.updatedAt)
        if (existing == null) {
            SettingsTable.insert {
                it[SettingsTable.userId] = userId
                it[SettingsTable.key] = key
                it[SettingsTable.value] = value
                it[SettingsTable.updatedAt] = updatedAt
                it[SettingsTable.serverUpdatedAt] = now
            }
        } else if (updatedAt > existing) {
            SettingsTable.update({ match() }) {
                it[SettingsTable.value] = value
                it[SettingsTable.updatedAt] = updatedAt
                it[SettingsTable.serverUpdatedAt] = now
            }
        }
    }

    private fun readSettings(userId: String): SettingsDto? =
        SettingsTable.selectAll()
            .where { (SettingsTable.userId eq userId) and (SettingsTable.key eq GAME_START_KEY) }
            .singleOrNull()?.let {
            SettingsDto(gameStart = it[SettingsTable.value], updatedAt = it[SettingsTable.updatedAt])
        }

    private fun UpdateBuilder<*>.fillQuest(userId: String, dto: QuestDto, now: Long) {
        this[QuestsTable.userId] = userId
        this[QuestsTable.id] = dto.id
        this[QuestsTable.title] = dto.title
        this[QuestsTable.difficulty] = dto.difficulty
        this[QuestsTable.weights] = dto.weights
        this[QuestsTable.days] = dto.days.joinToString(",")
        this[QuestsTable.createdOn] = dto.createdOn
        this[QuestsTable.targetAmount] = dto.targetAmount
        this[QuestsTable.targetUnit] = dto.targetUnit
        this[QuestsTable.note] = dto.note
        this[QuestsTable.category] = dto.category
        this[QuestsTable.choiceGroup] = dto.choiceGroup
        this[QuestsTable.tracksWin] = dto.tracksWin
        this[QuestsTable.programId] = dto.programId
        this[QuestsTable.archivedOn] = dto.archivedOn
        this[QuestsTable.sortOrder] = dto.sortOrder
        this[QuestsTable.updatedAt] = dto.updatedAt
        this[QuestsTable.serverUpdatedAt] = now
    }

    private fun UpdateBuilder<*>.fillCompletion(userId: String, dto: CompletionDto, now: Long) {
        this[CompletionsTable.userId] = userId
        this[CompletionsTable.id] = dto.id
        this[CompletionsTable.questId] = dto.questId
        this[CompletionsTable.questTitle] = dto.questTitle
        this[CompletionsTable.date] = dto.date
        this[CompletionsTable.completedAt] = dto.completedAt
        this[CompletionsTable.updatedAt] = dto.updatedAt
        this[CompletionsTable.slotKey] = dto.slotKey
        this[CompletionsTable.difficulty] = dto.difficulty
        this[CompletionsTable.weights] = dto.weights
        this[CompletionsTable.category] = dto.category
        this[CompletionsTable.targetAmount] = dto.targetAmount
        this[CompletionsTable.amount] = dto.amount
        this[CompletionsTable.won] = dto.won
        this[CompletionsTable.voided] = dto.voided
        this[CompletionsTable.serverUpdatedAt] = now
    }

    // --- 읽기 -------------------------------------------------------------

    private fun ResultRow.toQuestDto() = QuestDto(
        id = this[QuestsTable.id],
        title = this[QuestsTable.title],
        difficulty = this[QuestsTable.difficulty],
        weights = this[QuestsTable.weights],
        days = this[QuestsTable.days].split(",").mapNotNull { it.trim().toIntOrNull() },
        createdOn = this[QuestsTable.createdOn],
        targetAmount = this[QuestsTable.targetAmount],
        targetUnit = this[QuestsTable.targetUnit],
        note = this[QuestsTable.note],
        category = this[QuestsTable.category],
        choiceGroup = this[QuestsTable.choiceGroup],
        tracksWin = this[QuestsTable.tracksWin],
        programId = this[QuestsTable.programId],
        archivedOn = this[QuestsTable.archivedOn],
        sortOrder = this[QuestsTable.sortOrder],
        updatedAt = this[QuestsTable.updatedAt],
    )

    private fun ResultRow.toCompletionDto() = CompletionDto(
        id = this[CompletionsTable.id],
        questId = this[CompletionsTable.questId],
        questTitle = this[CompletionsTable.questTitle],
        date = this[CompletionsTable.date],
        completedAt = this[CompletionsTable.completedAt],
        updatedAt = this[CompletionsTable.updatedAt],
        slotKey = this[CompletionsTable.slotKey],
        difficulty = this[CompletionsTable.difficulty],
        weights = this[CompletionsTable.weights],
        category = this[CompletionsTable.category],
        targetAmount = this[CompletionsTable.targetAmount],
        amount = this[CompletionsTable.amount],
        won = this[CompletionsTable.won],
        voided = this[CompletionsTable.voided],
    )

    companion object {
        const val GAME_START_KEY = "game_start"
    }
}
