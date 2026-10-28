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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * 동기화 저장소. 규칙은 하나: 같은 id 면 updatedAt 이 큰 쪽이 이긴다.
 * 같은 요청을 두 번 받아도 결과가 같다 (재시도 안전).
 */
class SyncStore(
    private val database: Database,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun push(request: PushRequest): PushResponse = transaction(database) {
        val now = clock()
        val quests = request.quests.count { upsertQuest(it, now) }
        val completions = request.completions.count { upsertCompletion(it, now) }
        request.settings?.let { upsertSetting(GAME_START_KEY, it.gameStart, it.updatedAt, now) }
        PushResponse(acceptedQuests = quests, acceptedCompletions = completions, serverTime = now)
    }

    /** since 이후(같은 시각 포함)에 서버가 받은 행. 같은 행을 한 번 더 받아도 updatedAt 비교로 무해하다. */
    fun pull(since: Long): PullResponse = transaction(database) {
        val now = clock()
        PullResponse(
            quests = QuestsTable.selectAll()
                .where { QuestsTable.serverUpdatedAt greaterEq since }
                .map { it.toQuestDto() },
            completions = CompletionsTable.selectAll()
                .where { CompletionsTable.serverUpdatedAt greaterEq since }
                .map { it.toCompletionDto() },
            settings = readSettings(),
            serverTime = now,
        )
    }

    /** 엔진 계산용 전체 데이터. */
    fun snapshot(): Triple<List<QuestDto>, List<CompletionDto>, SettingsDto?> = transaction(database) {
        Triple(
            QuestsTable.selectAll().map { it.toQuestDto() },
            CompletionsTable.selectAll().map { it.toCompletionDto() },
            readSettings(),
        )
    }

    // --- 쓰기 -------------------------------------------------------------

    private fun upsertQuest(dto: QuestDto, now: Long): Boolean {
        val existing = QuestsTable.selectAll().where { QuestsTable.id eq dto.id }
            .singleOrNull()?.get(QuestsTable.updatedAt)
        return when {
            existing == null -> {
                QuestsTable.insert { it.fillQuest(dto, now) }
                true
            }
            dto.updatedAt > existing -> {
                QuestsTable.update({ QuestsTable.id eq dto.id }) { it.fillQuest(dto, now) }
                true
            }
            else -> false
        }
    }

    private fun upsertCompletion(dto: CompletionDto, now: Long): Boolean {
        val existing = CompletionsTable.selectAll().where { CompletionsTable.id eq dto.id }
            .singleOrNull()?.get(CompletionsTable.updatedAt)
        return when {
            existing == null -> {
                CompletionsTable.insert { it.fillCompletion(dto, now) }
                true
            }
            dto.updatedAt > existing -> {
                CompletionsTable.update({ CompletionsTable.id eq dto.id }) { it.fillCompletion(dto, now) }
                true
            }
            else -> false
        }
    }

    private fun upsertSetting(key: String, value: String, updatedAt: Long, now: Long) {
        val existing = SettingsTable.selectAll().where { SettingsTable.key eq key }
            .singleOrNull()?.get(SettingsTable.updatedAt)
        if (existing == null) {
            SettingsTable.insert {
                it[SettingsTable.key] = key
                it[SettingsTable.value] = value
                it[SettingsTable.updatedAt] = updatedAt
                it[SettingsTable.serverUpdatedAt] = now
            }
        } else if (updatedAt > existing) {
            SettingsTable.update({ SettingsTable.key eq key }) {
                it[SettingsTable.value] = value
                it[SettingsTable.updatedAt] = updatedAt
                it[SettingsTable.serverUpdatedAt] = now
            }
        }
    }

    private fun readSettings(): SettingsDto? =
        SettingsTable.selectAll().where { SettingsTable.key eq GAME_START_KEY }.singleOrNull()?.let {
            SettingsDto(gameStart = it[SettingsTable.value], updatedAt = it[SettingsTable.updatedAt])
        }

    private fun UpdateBuilder<*>.fillQuest(dto: QuestDto, now: Long) {
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

    private fun UpdateBuilder<*>.fillCompletion(dto: CompletionDto, now: Long) {
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
