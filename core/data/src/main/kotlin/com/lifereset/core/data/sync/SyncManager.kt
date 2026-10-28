package com.lifereset.core.data.sync

import androidx.room.withTransaction
import com.lifereset.core.data.db.LifeResetDatabase
import com.lifereset.core.data.db.SettingEntity
import com.lifereset.core.data.db.toDto
import com.lifereset.core.data.db.toEntity
import com.lifereset.core.data.repository.Account
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
    /** 서버 주소가 없거나 로그인 전 */
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
        }.getOrElse { failure(it) }

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
        }.getOrElse { failure(it) }
    }

    /** 서버 주소가 맞는지 확인하고, 로그인에 쓸 Google 클라이언트 ID 를 받아온다. */
    suspend fun checkServer(baseUrl: String): Result<ServerInfo> = runCatching {
        val health = api.health(baseUrl.trim().trimEnd('/'))
        ServerInfo(
            version = health.version,
            googleClientId = health.googleClientId ?: error("서버에 Google 클라이언트 ID 가 설정되지 않았습니다"),
        )
    }

    /**
     * Google ID 토큰으로 서버에 로그인하고, 이 폰의 기록과 서버의 기록을 합친다.
     *  - 서버에 이미 기록이 있으면(다른 폰에서 쓰던 계정) 받아와서 합친다 (최신 수정이 이김)
     *  - 게임 시작일은 둘 중 더 이른 날을 쓴다 (66일 진행이 짧아지지 않게)
     *  - 이 폰의 기록은 전부 새 계정으로 다시 올린다
     */
    suspend fun signIn(baseUrl: String, idToken: String): Result<Account> = runCatching {
        val url = baseUrl.trim().trimEnd('/')
        val auth = api.signInWithGoogle(url, idToken)
        val account = Account(auth.email, auth.displayName)
        settings.setServerUrl(url)
        settings.setSession(auth.sessionToken, account)

        mutex.withLock {
            val config = ServerConfig(url, auth.sessionToken)
            val localStart = settingDao.get(SettingKeys.GAME_START) // 합치기 전에 기억
            val remote = api.pull(config, since = 0L)
            merge(remote)
            remote.settings?.let { adoptEarlierGameStart(it, localStart) }
            questDao.markAllDirty()
            completionDao.markAllDirty()
            settings.setLastPull(remote.serverTime)
        }
        sync()
        account
    }

    /** 로그아웃: 서버 세션을 지우고 폰의 토큰을 지운다. 폰의 기록은 그대로 남는다. */
    suspend fun signOut() {
        settings.serverConfig()?.let { config -> runCatching { api.signOut(config) } }
        settings.clearSession()
        settings.setSyncStatus(SyncStatus(time.nowMillis(), true, "로그아웃했습니다"))
    }

    private suspend fun failure(error: Throwable): SyncResult.Failure {
        if (error is UnauthorizedException) settings.clearSession()
        return SyncResult.Failure(error.message ?: "알 수 없는 오류")
    }

    private suspend fun adoptEarlierGameStart(remote: SettingsDto, local: SettingEntity?) {
        if (local == null || remote.gameStart <= local.value) {
            settingDao.upsert(SettingEntity(SettingKeys.GAME_START, remote.gameStart, time.nowMillis(), dirty = false))
        } else {
            // 이 폰의 시작일이 더 이르다 → 서버로 올린다
            settingDao.upsert(local.copy(updatedAt = time.nowMillis(), dirty = true))
        }
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

/** 서버 연결 확인 결과. */
data class ServerInfo(val version: String, val googleClientId: String)
