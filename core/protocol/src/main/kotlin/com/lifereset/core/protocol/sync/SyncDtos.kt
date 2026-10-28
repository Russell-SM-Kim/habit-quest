package com.lifereset.core.protocol.sync

import kotlinx.serialization.Serializable

/*
 * 서버 동기화 형식.
 * - 날짜는 ISO 문자열 ("2026-10-27"), 시각은 epoch 밀리초.
 * - 충돌은 updatedAt 이 큰 쪽이 이긴다 (마지막에 고친 기록이 정답).
 * - serverUpdatedAt 은 서버가 받은 시각. 앱은 이 값을 커서로 써서 '그 뒤에 바뀐 것'만 받아온다.
 */

@Serializable
data class QuestDto(
    val id: String,
    val title: String,
    val difficulty: String,
    val weights: String,
    val days: List<Int>,
    val createdOn: String,
    val targetAmount: Double? = null,
    val targetUnit: String? = null,
    val note: String = "",
    val category: String,
    val choiceGroup: String? = null,
    val tracksWin: Boolean = false,
    val programId: String? = null,
    val archivedOn: String? = null,
    val sortOrder: Int = 0,
    val updatedAt: Long,
)

@Serializable
data class CompletionDto(
    val id: String,
    val questId: String,
    val questTitle: String,
    val date: String,
    val completedAt: Long,
    val updatedAt: Long,
    val slotKey: String,
    val difficulty: String,
    val weights: String,
    val category: String,
    val targetAmount: Double? = null,
    val amount: Double? = null,
    val won: Boolean = false,
    val voided: Boolean = false,
)

@Serializable
data class SettingsDto(
    /** 게임 시작일 (시즌 1 Day 1) */
    val gameStart: String,
    val updatedAt: Long,
)

@Serializable
data class PushRequest(
    val quests: List<QuestDto> = emptyList(),
    val completions: List<CompletionDto> = emptyList(),
    val settings: SettingsDto? = null,
)

@Serializable
data class PushResponse(
    val acceptedQuests: Int,
    val acceptedCompletions: Int,
    val serverTime: Long,
)

@Serializable
data class PullResponse(
    val quests: List<QuestDto>,
    val completions: List<CompletionDto>,
    val settings: SettingsDto?,
    /** 다음 pull 때 since 로 보낼 값 */
    val serverTime: Long,
)

/**
 * 연결 확인. googleClientId 는 서버에 설정된 Google 웹 클라이언트 ID.
 * 앱은 이 값으로 Google 로그인을 하므로, 클라이언트 ID 를 앱에 따로 넣을 필요가 없다.
 */
@Serializable
data class HealthDto(
    val ok: Boolean,
    val serverTime: Long,
    val version: String,
    val googleClientId: String? = null,
)

/** 서버가 같은 엔진으로 계산한 요약. 앱 계산과 비교해 검산하는 용도. */
@Serializable
data class ProfileDto(
    val level: Int,
    val rank: String,
    val playerClass: String,
    val totalXp: Long,
    val streak: Int,
    val bestStreak: Int,
    val season: Int,
    val seasonDay: Int,
    val stats: Map<String, Int>,
)
