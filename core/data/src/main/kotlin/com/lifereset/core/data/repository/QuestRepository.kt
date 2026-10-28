package com.lifereset.core.data.repository

import com.lifereset.core.data.db.QuestDao
import com.lifereset.core.data.db.toDomain
import com.lifereset.core.data.db.toEntity
import com.lifereset.core.data.sync.SyncRequester
import com.lifereset.core.data.time.TimeProvider
import com.lifereset.core.game.model.QuestDef
import com.lifereset.core.game.seed.DefaultQuests
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 퀘스트 정의 저장소. 보관(archive)된 퀘스트도 과거 기록 계산에 필요하므로 지우지 않는다. */
interface QuestRepository {
    /** 보관된 퀘스트까지 전부. */
    val quests: Flow<List<QuestDef>>

    suspend fun get(id: String): QuestDef?

    /** 새로 만들거나 고친다. */
    suspend fun save(quest: QuestDef)

    /** 오늘부터 더 이상 나오지 않게 한다. 과거 기록은 그대로. */
    suspend fun archive(id: String)

    suspend fun nextSortOrder(): Int

    /** 처음 실행이면 기본 퀘스트를 넣는다. */
    suspend fun seedIfEmpty(startDate: LocalDate)
}

internal class OfflineQuestRepository(
    private val dao: QuestDao,
    private val time: TimeProvider,
    private val sync: SyncRequester,
) : QuestRepository {

    override val quests: Flow<List<QuestDef>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: String): QuestDef? = dao.get(id)?.toDomain()

    override suspend fun save(quest: QuestDef) {
        dao.upsert(quest.toEntity(updatedAt = time.nowMillis(), dirty = true))
        sync.requestSync()
    }

    override suspend fun archive(id: String) {
        val quest = get(id) ?: return
        save(quest.copy(archivedOn = time.today()))
    }

    override suspend fun nextSortOrder(): Int = dao.maxSortOrder() + 1

    override suspend fun seedIfEmpty(startDate: LocalDate) {
        if (dao.count() > 0) return
        val now = time.nowMillis()
        dao.upsertAll(DefaultQuests.create(startDate).map { it.toEntity(updatedAt = now, dirty = true) })
        sync.requestSync()
    }
}
