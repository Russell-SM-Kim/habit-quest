package com.lifereset.core.data.repository

import com.lifereset.core.data.db.SettingDao
import com.lifereset.core.data.db.SettingEntity
import com.lifereset.core.data.sync.SyncRequester
import com.lifereset.core.data.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 서버 주소와 API 키. 폰에만 저장되고 서버로 올라가지 않는다. */
data class ServerConfig(val baseUrl: String, val apiKey: String) {
    val isValid: Boolean get() = baseUrl.startsWith("http") && apiKey.length >= 16
}

data class SyncStatus(val lastSyncAt: Long?, val ok: Boolean, val message: String) {
    companion object {
        val Never = SyncStatus(lastSyncAt = null, ok = true, message = "아직 동기화하지 않았습니다")
    }
}

interface SettingsRepository {
    val gameStart: Flow<LocalDate?>
    suspend fun gameStart(): LocalDate?
    suspend fun setGameStart(date: LocalDate)

    val playerName: Flow<String>
    suspend fun setPlayerName(name: String)

    val serverConfig: Flow<ServerConfig?>
    suspend fun serverConfig(): ServerConfig?
    suspend fun setServerConfig(config: ServerConfig)

    val syncStatus: Flow<SyncStatus>
    suspend fun setSyncStatus(status: SyncStatus)

    /** 배경 이미지 content:// URI. null 이면 기본 하늘 배경. */
    val backgroundUri: Flow<String?>
    suspend fun setBackgroundUri(uri: String?)

    /** 효과음 켜기/끄기 (기본 켜짐). */
    val soundEnabled: Flow<Boolean>
    suspend fun setSoundEnabled(enabled: Boolean)

    /** 마지막으로 서버에서 받아온 시각(서버 기준). */
    suspend fun lastPull(): Long
    suspend fun setLastPull(serverTime: Long)
}

internal object SettingKeys {
    const val GAME_START = "game_start" // 서버와 공유
    const val PLAYER_NAME = "player_name"
    const val SERVER_URL = "server_url"
    const val API_KEY = "api_key"
    const val LAST_PULL = "last_pull"
    const val SYNC_AT = "sync_at"
    const val SYNC_OK = "sync_ok"
    const val SYNC_MESSAGE = "sync_message"
    const val BACKGROUND_URI = "background_uri"
    const val SOUND_ENABLED = "sound_enabled"
}

internal class LocalSettingsRepository(
    private val dao: SettingDao,
    private val time: TimeProvider,
    private val sync: SyncRequester,
) : SettingsRepository {

    override val gameStart: Flow<LocalDate?> =
        dao.observe(SettingKeys.GAME_START).map { it?.value?.let(LocalDate::parse) }

    override suspend fun gameStart(): LocalDate? = dao.get(SettingKeys.GAME_START)?.value?.let(LocalDate::parse)

    override suspend fun setGameStart(date: LocalDate) {
        put(SettingKeys.GAME_START, date.toString(), shared = true)
        sync.requestSync()
    }

    override val playerName: Flow<String> =
        dao.observe(SettingKeys.PLAYER_NAME).map { it?.value ?: "플레이어" }

    override suspend fun setPlayerName(name: String) = put(SettingKeys.PLAYER_NAME, name.trim())

    override val serverConfig: Flow<ServerConfig?> = combine(
        dao.observe(SettingKeys.SERVER_URL),
        dao.observe(SettingKeys.API_KEY),
    ) { url, key -> if (url == null || key == null) null else ServerConfig(url.value, key.value) }

    override suspend fun serverConfig(): ServerConfig? {
        val url = dao.get(SettingKeys.SERVER_URL)?.value ?: return null
        val key = dao.get(SettingKeys.API_KEY)?.value ?: return null
        return ServerConfig(url, key)
    }

    override suspend fun setServerConfig(config: ServerConfig) {
        put(SettingKeys.SERVER_URL, config.baseUrl.trim().trimEnd('/'))
        put(SettingKeys.API_KEY, config.apiKey.trim())
        sync.requestSync()
    }

    override val syncStatus: Flow<SyncStatus> = combine(
        dao.observe(SettingKeys.SYNC_AT),
        dao.observe(SettingKeys.SYNC_OK),
        dao.observe(SettingKeys.SYNC_MESSAGE),
    ) { at, ok, message ->
        if (at == null) {
            SyncStatus.Never
        } else {
            SyncStatus(at.value.toLongOrNull(), ok?.value != "false", message?.value.orEmpty())
        }
    }

    override suspend fun setSyncStatus(status: SyncStatus) {
        put(SettingKeys.SYNC_OK, status.ok.toString())
        put(SettingKeys.SYNC_MESSAGE, status.message)
        put(SettingKeys.SYNC_AT, (status.lastSyncAt ?: time.nowMillis()).toString())
    }

    override val backgroundUri: Flow<String?> =
        dao.observe(SettingKeys.BACKGROUND_URI).map { it?.value?.takeIf(String::isNotBlank) }

    override suspend fun setBackgroundUri(uri: String?) = put(SettingKeys.BACKGROUND_URI, uri.orEmpty())

    override val soundEnabled: Flow<Boolean> =
        dao.observe(SettingKeys.SOUND_ENABLED).map { it?.value != "false" }

    override suspend fun setSoundEnabled(enabled: Boolean) = put(SettingKeys.SOUND_ENABLED, enabled.toString())

    override suspend fun lastPull(): Long = dao.get(SettingKeys.LAST_PULL)?.value?.toLongOrNull() ?: 0L

    override suspend fun setLastPull(serverTime: Long) = put(SettingKeys.LAST_PULL, serverTime.toString())

    private suspend fun put(key: String, value: String, shared: Boolean = false) {
        dao.upsert(SettingEntity(key = key, value = value, updatedAt = time.nowMillis(), dirty = shared))
    }
}
