package com.lifereset.core.data.repository

import com.lifereset.core.data.db.CompletionDao
import com.lifereset.core.data.db.toDomain
import com.lifereset.core.data.db.toEntity
import com.lifereset.core.data.sync.SyncRequester
import com.lifereset.core.game.model.Completion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 완료 기록 저장소. 취소도 지우지 않고 voided 로 저장한다. */
interface CompletionRepository {
    /** 취소된 기록까지 전부. 엔진이 알아서 거른다. */
    val completions: Flow<List<Completion>>

    suspend fun get(id: String): Completion?

    suspend fun save(completion: Completion)
}

internal class OfflineCompletionRepository(
    private val dao: CompletionDao,
    private val sync: SyncRequester,
) : CompletionRepository {

    override val completions: Flow<List<Completion>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: String): Completion? = dao.get(id)?.toDomain()

    override suspend fun save(completion: Completion) {
        dao.upsert(completion.toEntity(dirty = true))
        sync.requestSync()
    }
}
