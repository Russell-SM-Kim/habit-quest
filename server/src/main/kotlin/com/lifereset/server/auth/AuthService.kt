package com.lifereset.server.auth

import com.lifereset.core.protocol.auth.AuthResponse
import com.lifereset.server.config.ServerConfig
import com.lifereset.server.db.SessionsTable
import com.lifereset.server.db.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** 로그인 실패 사유. 라우트에서 401/403 으로 바꾼다. */
class AuthException(val forbidden: Boolean, message: String) : Exception(message)

/**
 * Google 로그인 → 사용자 등록 → 세션 토큰 발급/확인/삭제.
 * 세션 토큰은 32바이트 난수. 만료 없이 로그아웃할 때까지 유효하다 (혼자 쓰는 앱).
 */
class AuthService(
    private val database: Database,
    private val config: ServerConfig,
    private val verifier: IdTokenVerifier,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val random = SecureRandom()

    suspend fun signInWithGoogle(idToken: String): AuthResponse {
        val identity = verifier.verify(idToken)
            ?: throw AuthException(forbidden = false, message = "Google 토큰을 확인할 수 없습니다")
        if (!config.isAllowed(identity.email)) {
            throw AuthException(forbidden = true, message = "${identity.email} 은(는) 이 서버에 허용되지 않은 계정입니다")
        }
        val token = newToken()
        transaction(database) {
            val now = clock()
            val exists = UsersTable.selectAll().where { UsersTable.id eq identity.subject }.any()
            if (exists) {
                UsersTable.update({ UsersTable.id eq identity.subject }) {
                    it[email] = identity.email
                    it[displayName] = identity.displayName
                    it[lastLoginAt] = now
                }
            } else {
                UsersTable.insert {
                    it[id] = identity.subject
                    it[email] = identity.email
                    it[displayName] = identity.displayName
                    it[createdAt] = now
                    it[lastLoginAt] = now
                }
            }
            SessionsTable.insert {
                it[tokenHash] = hash(token)
                it[userId] = identity.subject
                it[createdAt] = now
                it[lastUsedAt] = now
            }
        }
        return AuthResponse(sessionToken = token, email = identity.email, displayName = identity.displayName)
    }

    /** 세션 토큰 → 사용자 id. 없으면 null. */
    fun userIdFor(token: String): String? = transaction(database) {
        val key = hash(token)
        val userId = SessionsTable.selectAll().where { SessionsTable.tokenHash eq key }
            .singleOrNull()?.get(SessionsTable.userId) ?: return@transaction null
        SessionsTable.update({ SessionsTable.tokenHash eq key }) { it[lastUsedAt] = clock() }
        userId
    }

    fun signOut(token: String) {
        transaction(database) {
            val key = hash(token)
            SessionsTable.deleteWhere { tokenHash eq key }
        }
    }

    private fun newToken(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
