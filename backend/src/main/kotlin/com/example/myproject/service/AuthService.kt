package com.example.myproject.service

import com.example.myproject.configuration.AppConfig
import com.example.myproject.configuration.JwtConfig
import com.example.myproject.domain.RefreshTokensTable
import com.example.myproject.domain.User
import com.example.myproject.domain.UsersTable
import com.example.myproject.dto.AuthResponse
import com.example.myproject.dto.UserResponse
import com.example.myproject.plugins.dbQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Registration, login, and session management.
 *
 * Access tokens are short-lived stateless JWTs. Refresh tokens are JWTs whose id (`jti`) is
 * recorded in `refresh_tokens`: every refresh rotates the token (the presented one is revoked and
 * a new one issued), and presenting an already-revoked token is treated as theft, revoking every
 * active refresh token of that user.
 */
class AuthService(
    private val database: Database,
    private val jwt: JwtConfig,
    private val passwordHashCost: Int = AppConfig.DEFAULT_PASSWORD_HASH_COST,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val tokens = JwtTokens(jwt)

    /** Compared against when an email is unknown, so login timing does not reveal accounts. */
    private val dummyHash: String = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(passwordHashCost))

    /**
     * Creates an account and signs it in. Returns null when the email is already registered.
     * @throws ValidationException for invalid input.
     */
    suspend fun register(email: String, password: String, displayName: String): AuthResponse? {
        val normalizedEmail = InputRules.email(email)
        InputRules.password(password)
        val name = InputRules.displayName(displayName)
        if (findUserByEmail(normalizedEmail) != null) return null

        val passwordHash = withContext(Dispatchers.Default) {
            BCrypt.hashpw(password, BCrypt.gensalt(passwordHashCost))
        }
        return database.dbQuery {
            val now = clock.instant()
            val userId = UsersTable.insertIgnoreAndGetId {
                it[UsersTable.email] = normalizedEmail
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.displayName] = name
                it[UsersTable.createdAt] = now
                it[UsersTable.updatedAt] = now
            }?.value
            userId?.let { issueTokens(User(it, normalizedEmail, passwordHash, name, now, now), now) }
        }
    }

    /** Returns tokens for valid credentials, otherwise null. */
    suspend fun login(email: String, password: String): AuthResponse? {
        val user = findUserByEmail(InputRules.normalizeEmail(email))
        val passwordMatches = withContext(Dispatchers.Default) {
            BCrypt.checkpw(password, user?.passwordHash ?: dummyHash)
        }
        if (user == null || !passwordMatches) return null
        return database.dbQuery { issueTokens(user, clock.instant()) }
    }

    /**
     * Rotates [refreshToken]. Returns null when the token is invalid, expired, revoked, or its
     * user no longer exists. Reuse of a revoked token revokes all of that user's refresh tokens.
     */
    suspend fun refresh(refreshToken: String): AuthResponse? {
        val claims = tokens.decodeRefreshToken(refreshToken) ?: return null
        val now = clock.instant()
        return database.dbQuery {
            val rotated = RefreshTokensTable.update({
                (RefreshTokensTable.id eq claims.tokenId) and
                    (RefreshTokensTable.userId eq claims.userId) and
                    RefreshTokensTable.revokedAt.isNull() and
                    (RefreshTokensTable.expiresAt greater now)
            }) {
                it[RefreshTokensTable.revokedAt] = now
            }
            if (rotated == 1) {
                findUserRow(claims.userId)?.let { user -> issueTokens(user, now) }
            } else {
                revokeAllIfReused(claims, now)
                null
            }
        }
    }

    /** Revokes [refreshToken] if it is valid and active. Idempotent. */
    suspend fun logout(refreshToken: String) {
        val claims = tokens.decodeRefreshToken(refreshToken) ?: return
        val now = clock.instant()
        database.dbQuery {
            RefreshTokensTable.update({
                (RefreshTokensTable.id eq claims.tokenId) and
                    (RefreshTokensTable.userId eq claims.userId) and
                    RefreshTokensTable.revokedAt.isNull()
            }) {
                it[RefreshTokensTable.revokedAt] = now
            }
        }
    }

    suspend fun findUserById(id: Long): User? = database.dbQuery { findUserRow(id) }

    /**
     * Updates the profile; a null [displayName] leaves it unchanged. Returns null for unknown users.
     * @throws ValidationException for invalid input.
     */
    suspend fun updateProfile(userId: Long, displayName: String?): User? {
        val name = displayName?.let(InputRules::displayName)
        return database.dbQuery {
            val updated = UsersTable.update({ UsersTable.id eq userId }) { row ->
                name?.let { row[UsersTable.displayName] = it }
                row[UsersTable.updatedAt] = clock.instant()
            }
            if (updated == 0) null else findUserRow(userId)
        }
    }

    /** Deletes the account; tasks and refresh tokens cascade. Returns false for unknown users. */
    suspend fun deleteAccount(userId: Long): Boolean {
        val deleted = database.dbQuery { UsersTable.deleteWhere { UsersTable.id eq userId } } > 0
        if (deleted) logger.info("Deleted account {}", userId)
        return deleted
    }

    private suspend fun findUserByEmail(email: String): User? = database.dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    private fun revokeAllIfReused(claims: RefreshClaims, now: Instant) {
        val reused = !RefreshTokensTable.selectAll().where {
            (RefreshTokensTable.id eq claims.tokenId) and
                (RefreshTokensTable.userId eq claims.userId) and
                RefreshTokensTable.revokedAt.isNotNull()
        }.empty()
        if (!reused) return

        val revoked = RefreshTokensTable.update({
            (RefreshTokensTable.userId eq claims.userId) and RefreshTokensTable.revokedAt.isNull()
        }) {
            it[RefreshTokensTable.revokedAt] = now
        }
        logger.warn("Refresh token reuse detected for user {}; revoked {} active token(s)", claims.userId, revoked)
    }

    /** Must run inside a transaction: records the new refresh token and prunes expired ones. */
    private fun issueTokens(user: User, now: Instant): AuthResponse {
        RefreshTokensTable.deleteWhere {
            (RefreshTokensTable.userId eq user.id) and (RefreshTokensTable.expiresAt lessEq now)
        }
        val tokenId = UUID.randomUUID().toString()
        val refreshExpiresAt = now.plusMillis(jwt.refreshExpirationMs)
        RefreshTokensTable.insert {
            it[RefreshTokensTable.id] = tokenId
            it[RefreshTokensTable.userId] = user.id
            it[RefreshTokensTable.createdAt] = now
            it[RefreshTokensTable.expiresAt] = refreshExpiresAt
        }

        return AuthResponse(
            accessToken = tokens.accessToken(user, now),
            refreshToken = tokens.refreshToken(user.id, tokenId, now, refreshExpiresAt),
            user = user.toResponse(),
        )
    }
}

fun User.toResponse() = UserResponse(id = id, email = email, displayName = displayName)

/** Must run inside a transaction. */
private fun findUserRow(id: Long): User? =
    UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()

private fun ResultRow.toUser() = User(
    id = this[UsersTable.id].value,
    email = this[UsersTable.email],
    passwordHash = this[UsersTable.passwordHash],
    displayName = this[UsersTable.displayName],
    createdAt = this[UsersTable.createdAt],
    updatedAt = this[UsersTable.updatedAt],
)
