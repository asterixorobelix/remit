package com.example.myproject.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.myproject.configuration.JwtConfig
import com.example.myproject.domain.User
import java.time.Instant

private const val TOKEN_TYPE_CLAIM = "type"
private const val ACCESS_TOKEN_TYPE = "access"
private const val REFRESH_TOKEN_TYPE = "refresh"

/** Identity carried by a verified refresh token. */
data class RefreshClaims(val userId: Long, val tokenId: String)

/**
 * Signs and verifies HS256 JWTs. Access and refresh tokens are distinguished by the `type` claim,
 * so neither can be used in place of the other.
 */
class JwtTokens(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    /** Verifier for bearer access tokens, used by the `auth-jwt` authentication provider. */
    val accessVerifier: JWTVerifier = verifier(ACCESS_TOKEN_TYPE)
    private val refreshVerifier: JWTVerifier = verifier(REFRESH_TOKEN_TYPE)

    fun accessToken(user: User, now: Instant): String = JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withSubject(user.id.toString())
        .withClaim("email", user.email)
        .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
        .withIssuedAt(now)
        .withExpiresAt(now.plusMillis(config.expirationMs))
        .sign(algorithm)

    fun refreshToken(userId: Long, tokenId: String, now: Instant, expiresAt: Instant): String = JWT.create()
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .withSubject(userId.toString())
        .withJWTId(tokenId)
        .withClaim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
        .withIssuedAt(now)
        .withExpiresAt(expiresAt)
        .sign(algorithm)

    /** Returns the claims of a validly signed, unexpired refresh token, otherwise null. */
    fun decodeRefreshToken(token: String): RefreshClaims? = try {
        val decoded = refreshVerifier.verify(token)
        val userId = decoded.subject?.toLongOrNull()
        val tokenId = decoded.id
        if (userId != null && tokenId != null) RefreshClaims(userId, tokenId) else null
    } catch (_: JWTVerificationException) {
        null
    }

    private fun verifier(type: String): JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withClaim(TOKEN_TYPE_CLAIM, type)
        .build()
}
