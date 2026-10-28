package com.lifereset.core.data.repository

import com.lifereset.core.data.BuildConfig
import com.lifereset.core.data.db.SettingDao
import com.lifereset.core.data.db.SettingEntity
import com.lifereset.core.data.sync.SyncRequester
import com.lifereset.core.data.time.TimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** 서버 주소 + 로그인 세션 토큰. 폰에만 저장되고 서버로 올라가지 않는다. */
data class ServerConfig(val baseUrl: String, val sessionToken: String) {
    val isValid: Boolean get() = baseUrl.startsWith("http") && sessionToken.isNotBlank()
}

/** 로그인한 Google 계정 (화면 표시용). */
data class Account(val email: String, val displayName: String)

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

    /**
     * 서버 주소 (예: https://내PC.tailnet.ts.net). 로그인 전에도 저장된다.
     * 직접 저장한 값이 없으면 앱에 내장된 기본 주소(local.properties 의 habitquest.serverUrl).
     */
    val serverUrl: Flow<String>
    suspend fun serverUrl(): String
    suspend fun setServerUrl(url: String)

    /** 로그인한 계정. 로그아웃 상태면 null. */
    val account: Flow<Account?>

    /** 시작 화면을 이미 지나갔는지 (로그인했거나 '로그인 없이 시작'을 눌렀음). */
    val onboardingDone: Flow<Boolean>
    suspend fun setOnboardingDone()

    /** 주소 + 세션 토큰. 로그인 전이면 null. 동기화는 이 값만 본다. */
    suspend fun serverConfig(): ServerConfig?
    suspend fun setSession(token: String, account: Account)
    suspend fun clearSession()

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
    const val SESSION_TOKEN = "session_token"
    const val ACCOUNT_EMAIL = "account_email"
    const val ACCOUNT_NAME = "account_name"
    const val LAST_PULL = "last_pull"
    const val SYNC_AT = "sync_at"
    const val SYNC_OK = "sync_ok"
    const val SYNC_MESSAGE = "sync_message"
    const val BACKGROUND_URI = "background_uri"
    const val SOUND_ENABLED = "sound_enabled"
    const val ONBOARDING_DONE = "onboarding_done"
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

    override val serverUrl: Flow<String> =
        dao.observe(SettingKeys.SERVER_URL).map { it?.value.orDefaultServer() }

    override suspend fun serverUrl(): String = dao.get(SettingKeys.SERVER_URL)?.value.orDefaultServer()

    private fun String?.orDefaultServer(): String = this?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_SERVER_URL

    override suspend fun setServerUrl(url: String) = put(SettingKeys.SERVER_URL, url.trim().trimEnd('/'))

    override val account: Flow<Account?> = combine(
        dao.observe(SettingKeys.ACCOUNT_EMAIL),
        dao.observe(SettingKeys.ACCOUNT_NAME),
    ) { email, name ->
        email?.value?.takeIf { it.isNotBlank() }?.let { Account(it, name?.value.orEmpty()) }
    }

    override val onboardingDone: Flow<Boolean> = combine(
        dao.observe(SettingKeys.ONBOARDING_DONE),
        account,
    ) { flag, account -> flag?.value == "true" || account != null }

    override suspend fun setOnboardingDone() = put(SettingKeys.ONBOARDING_DONE, "true")

    override suspend fun serverConfig(): ServerConfig? {
        val url = serverUrl().takeIf { it.isNotBlank() } ?: return null
        val token = dao.get(SettingKeys.SESSION_TOKEN)?.value?.takeIf { it.isNotBlank() } ?: return null
        return ServerConfig(url, token)
    }

    override suspend fun setSession(token: String, account: Account) {
        put(SettingKeys.SESSION_TOKEN, token)
        put(SettingKeys.ACCOUNT_NAME, account.displayName)
        put(SettingKeys.ACCOUNT_EMAIL, account.email)
    }

    override suspend fun clearSession() {
        put(SettingKeys.SESSION_TOKEN, "")
        put(SettingKeys.ACCOUNT_EMAIL, "")
        put(SettingKeys.ACCOUNT_NAME, "")
        put(SettingKeys.LAST_PULL, "0")
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
