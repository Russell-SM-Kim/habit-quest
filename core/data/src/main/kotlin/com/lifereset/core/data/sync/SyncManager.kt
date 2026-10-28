package com.lifereset.core.data.sync

import androidx.room.withTransaction
import com.lifereset.core.data.db.LifeResetDatabase
import com.lifereset.core.data.db.SettingEntity
import com.lifereset.core.data.db.toDto
import com.lifereset.core.data.db.toEntity
import com.lifereset.core.data.repository.ServerConfig
import com.lifereset.core.data.repository.SettingKeys
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.data.repository.SyncStatus
import com.lifereset.core.data.time.TimeProvider
import com.lifereset.core.protocol.sync.PullResponse
import com.lifereset.core.protocol.sync.PushRequest
import com.lifereset.core.protocol.sync.SettingsDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncResult {
    data class Success(val pushed: Int, val pulled: Int) : SyncResult
    data object NotConfigured : SyncResult
    data class Failure(val message: String) : SyncResult
}

/**
 * 1) 폰에서 바뀐 것(dirty)을 올리고  2) 서버에서 바뀐 것을 받아 합친다.
 * 합칠 때 규칙: updatedAt 이 더 큰 쪽이 이긴다.
 */
class SyncManager internal constructor(
    private val database: LifeResetDatabase,
    private val api: ServerApi,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()
    private val questDao get() = database.questDao()
    private val completionDao get() = database.completionDao()
    private val settingDao get() = database.settingDao()

    suspend fun sync(): SyncResult = mutex.withLock {
        val config = settings.serverConfig()?.takeIf { it.isValid } ?: return SyncResult.NotConfigured
        val result = runCatching {
            val pushed = pushDirty(config)
            val pulled = pullSince(config, settings.lastPull())
            SyncResult.Success(pushed, pulled)
        }.getOrElse { SyncResult.Failure(it.message ?: "알 수 없는 오류") }

        settings.setSyncStatus(
            when (result) {
                is SyncResult.Success -> SyncStatus(time.nowMillis(), true, "올림 ${result.pushed} · 받음 ${result.pulled}")
                is SyncResult.Failure -> SyncStatus(time.nowMillis(), false, result.message)
                SyncResult.NotConfigured -> SyncStatus.Never
            },
        )
        result
    }

    /** 새 폰이나 재설치 후: 서버의 전체 기록을 받아오고, 시작일은 서버 값을 따른다. */
    suspend fun restoreFromServer(): SyncResult = mutex.withLock {
        val config = settings.serverConfig()?.takeIf { it.isValid } ?: return SyncResult.NotConfigured
        runCatching {
            val response = api.pull(config, since = 0L)
            response.settings?.let { forceGameStart(it) }
            val pulled = merge(response, force = true)
            settings.setLastPull(response.serverTime)
            SyncResult.Success(0, pulled)
        }.getOrElse { SyncResult.Failure(it.message ?: "알 수 없는 오류") }
    }

    suspend fun testConnection(): Result<String> = runCatching {
        val config = settings.serverConfig() ?: error("서버 주소와 키를 먼저 입력하세요")
        val health = api.health(config)
        "연결됨 (서버 v${health.version})"
    }

    private suspend fun pushDirty(config: ServerConfig): Int {
        val quests = questDao.dirty()
        val completions = completionDao.dirty()
        val start = settingDao.get(SettingKeys.GAME_START)?.takeIf { it.dirty }
        if (quests.isEmpty() && completions.isEmpty() && start == null) return 0

        api.push(
            config,
            PushRequest(
                quests = quests.map { it.toDto() },
                completions = completions.map { it.toDto() },
                settings = start?.let { SettingsDto(it.value, it.updatedAt) },
            ),
        )
        // 올리는 동안 또 바뀐 행은 updatedAt 이 달라서 dirty 로 남는다 (다음에 다시 올림)
        quests.forEach { questDao.markClean(it.id, it.updatedAt) }
        completions.forEach { completionDao.markClean(it.id, it.updatedAt) }
        start?.let { settingDao.markClean(it.key, it.updatedAt) }
        return quests.size + completions.size
    }

    private suspend fun pullSince(config: ServerConfig, since: Long): Int {
        val response = api.pull(config, since)
        val merged = merge(response)
        settings.setLastPull(response.serverTime)
        return merged
    }

    /** force = true 면 서버 값으로 덮어쓴다 (복원). */
    private suspend fun merge(response: PullResponse, force: Boolean = false): Int = database.withTransaction {
        var changed = 0
        response.quests.forEach { remote ->
            val local = questDao.get(remote.id)
            if (force || local == null || remote.updatedAt > local.updatedAt) {
                questDao.upsert(remote.toEntity(dirty = false))
                changed++
            }
        }
        response.completions.forEach { remote ->
            val local = completionDao.get(remote.id)
            if (force || local == null || remote.updatedAt > local.updatedAt) {
                completionDao.upsert(remote.toEntity(dirty = false))
                changed++
            }
        }
        response.settings?.let { remote ->
            val local = settingDao.get(SettingKeys.GAME_START)
            if (local == null || remote.updatedAt > local.updatedAt) {
                settingDao.upsert(SettingEntity(SettingKeys.GAME_START, remote.gameStart, remote.updatedAt, dirty = false))
            }
        }
        changed
    }

    private suspend fun forceGameStart(remote: SettingsDto) {
        settingDao.upsert(SettingEntity(SettingKeys.GAME_START, remote.gameStart, remote.updatedAt, dirty = false))
    }
}
