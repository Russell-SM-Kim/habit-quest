package com.lifereset.server.db

import org.jetbrains.exposed.sql.Table

/*
 * 사용자(Google 계정)마다 기록이 따로 저장된다. 모든 기록 테이블은 user_id 를 가진다.
 * 레벨·스탯은 저장하지 않는다 (기록에서 계산).
 * server_updated_at: 서버가 이 행을 마지막으로 받은 시각. pull 커서로 쓴다.
 */

/** Google 계정. id = Google 의 고유 번호(sub), 이메일이 바뀌어도 그대로다. */
object UsersTable : Table("users") {
    val id = varchar("id", 64)
    val email = varchar("email", 320).index()
    val displayName = varchar("display_name", 200).default("")
    val createdAt = long("created_at")
    val lastLoginAt = long("last_login_at")

    override val primaryKey = PrimaryKey(id)
}

/** 로그인 세션. 토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다 (DB 가 새도 토큰을 못 쓰게). */
object SessionsTable : Table("sessions") {
    val tokenHash = varchar("token_hash", 64)
    val userId = varchar("user_id", 64).references(UsersTable.id).index()
    val createdAt = long("created_at")
    val lastUsedAt = long("last_used_at")

    override val primaryKey = PrimaryKey(tokenHash)
}

object QuestsTable : Table("quests") {
    val userId = varchar("user_id", 64).references(UsersTable.id)
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

    override val primaryKey = PrimaryKey(userId, id)
}

object CompletionsTable : Table("completions") {
    val userId = varchar("user_id", 64).references(UsersTable.id)
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

    override val primaryKey = PrimaryKey(userId, id)
}

/** 사용자별 키-값 설정 (게임 시작일 등). */
object SettingsTable : Table("settings") {
    val userId = varchar("user_id", 64).references(UsersTable.id)
    val key = varchar("key", 64)
    val value = varchar("value", 500)
    val updatedAt = long("updated_at")
    val serverUpdatedAt = long("server_updated_at")

    override val primaryKey = PrimaryKey(userId, key)
}
