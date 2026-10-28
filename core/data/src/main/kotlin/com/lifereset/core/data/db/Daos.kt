package com.lifereset.core.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY sortOrder, title")
    fun observeAll(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id")
    suspend fun get(id: String): QuestEntity?

    @Query("SELECT COUNT(*) FROM quests")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM quests")
    suspend fun maxSortOrder(): Int

    @Upsert
    suspend fun upsert(entity: QuestEntity)

    @Upsert
    suspend fun upsertAll(entities: List<QuestEntity>)

    @Query("SELECT * FROM quests WHERE dirty = 1")
    suspend fun dirty(): List<QuestEntity>

    /** 올리는 사이에 또 고쳐졌다면(updatedAt 이 다르면) dirty 를 유지한다. */
    @Query("UPDATE quests SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions ORDER BY completedAt")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE id = :id")
    suspend fun get(id: String): CompletionEntity?

    @Upsert
    suspend fun upsert(entity: CompletionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<CompletionEntity>)

    @Query("SELECT * FROM completions WHERE dirty = 1")
    suspend fun dirty(): List<CompletionEntity>

    @Query("UPDATE completions SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markClean(id: String, updatedAt: Long)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<SettingEntity?>

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun get(key: String): SettingEntity?

    @Upsert
    suspend fun upsert(entity: SettingEntity)

    @Query("UPDATE settings SET dirty = 0 WHERE `key` = :key AND updatedAt = :updatedAt")
    suspend fun markClean(key: String, updatedAt: Long)
}
