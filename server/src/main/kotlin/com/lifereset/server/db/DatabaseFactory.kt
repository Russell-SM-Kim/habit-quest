package com.lifereset.server.db

import com.lifereset.server.config.ServerConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    /** DB 에 연결하고, 없는 테이블·컬럼은 만든다 (앱 업데이트로 컬럼이 늘어도 자동 반영). */
    fun connect(config: ServerConfig): Database {
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.dbUrl
                username = config.dbUser
                password = config.dbPassword
                maximumPoolSize = 4
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            },
        )
        val database = Database.connect(dataSource)
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(UsersTable, SessionsTable, QuestsTable, CompletionsTable, SettingsTable)
        }
        return database
    }
}
