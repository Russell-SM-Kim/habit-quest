package com.lifereset.server.db

import org.jetbrains.exposed.sql.Table

/*
 * 테이블은 앱의 Room 테이블과 같은 모양이다. 레벨·스탯은 저장하지 않는다 (기록에서 계산).
 * server_updated_at: 서버가 이 행을 마지막으로 받은 시각. pull 커서로 쓴다.
 */

object QuestsTable : Table("quests") {
    val id = varchar("id", 64)
    val title = varchar("title", 200)
    val difficulty = varchar("difficulty", 16)
    val weights = varchar("weights", 200)
    val days = varchar("days", 32) // "1,2,3"
    val createdOn = varchar("created_on", 10)
    val targetAmount = double("target_amount").nullable()
    val targetUnit = varchar("target_unit", 16).nullable()
    val note = varchar("note", 500).default("")
    val category = varchar("category", 16)
    val choiceGroup = varchar("choice_group", 64).nullable()
    val tracksWin = bool("tracks_win").default(false)
    val programId = varchar("program_id", 64).nullable()
    val archivedOn = varchar("archived_on", 10).nullable()
    val sortOrder = integer("sort_order").default(0)
    val updatedAt = long("updated_at")
    val serverUpdatedAt = long("server_updated_at").index()

    override val primaryKey = PrimaryKey(id)
}

object CompletionsTable : Table("completions") {
    val id = varchar("id", 64)
    val questId = varchar("quest_id", 64).index()
    val questTitle = varchar("quest_title", 200)
    val date = varchar("date", 10).index()
    val completedAt = long("completed_at")
    val updatedAt = long("updated_at")
    val slotKey = varchar("slot_key", 64)
    val difficulty = varchar("difficulty", 16)
    val weights = varchar("weights", 200)
    val category = varchar("category", 16)
    val targetAmount = double("target_amount").nullable()
    val amount = double("amount").nullable()
    val won = bool("won").default(false)
    val voided = bool("voided").default(false)
    val serverUpdatedAt = long("server_updated_at").index()

    override val primaryKey = PrimaryKey(id)
}

/** 키-값 설정 (게임 시작일 등). */
object SettingsTable : Table("settings") {
    val key = varchar("key", 64)
    val value = varchar("value", 500)
    val updatedAt = long("updated_at")
    val serverUpdatedAt = long("server_updated_at")

    override val primaryKey = PrimaryKey(key)
}
