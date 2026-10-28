package com.lifereset.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Room 테이블. 서버 테이블과 같은 모양이고, dirty = 아직 서버에 올리지 않은 변경.
 * 컬럼을 추가하면 LifeResetDatabase 의 version 을 올리고 마이그레이션을 추가한다.
 */

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val difficulty: String,
    val weights: String,
    /** DayOfWeek.value 를 쉼표로 이은 값 ("1,3,5" = 월수금) */
    val days: String,
    val createdOn: String,
    val targetAmount: Double?,
    val targetUnit: String?,
    val note: String,
    val category: String,
    val choiceGroup: String?,
    val tracksWin: Boolean,
    val programId: String?,
    val archivedOn: String?,
    val sortOrder: Int,
    val updatedAt: Long,
    val dirty: Boolean,
)

@Entity(
    tableName = "completions",
    indices = [Index("date"), Index("questId")],
)
data class CompletionEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val questTitle: String,
    val date: String,
    val completedAt: Long,
    val updatedAt: Long,
    val slotKey: String,
    val difficulty: String,
    val weights: String,
    val category: String,
    val targetAmount: Double?,
    val amount: Double?,
    val won: Boolean,
    val voided: Boolean,
    val dirty: Boolean,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long,
    /** 서버와 공유하는 설정만 true 가 된다 (게임 시작일). 서버 주소·키는 폰에만 둔다. */
    val dirty: Boolean,
)
