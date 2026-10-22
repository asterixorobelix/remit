package com.asterixorobelix.remit

import com.asterixorobelix.remit.configuration.RateLimitConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AuthRoutesTest {
    @Test
    fun `registration treats email case variants as the same account`() = testApplication {
        startApp()
        val localPart = UUID.randomUUID().toString()
        client.register(email = "$localPart@Example.com")

        val duplicate = client.postJson(
            "/api/v1/auth/register",
            json("email" to " $localPart@example.COM ", "password" to TEST_PASSWORD, "displayName" to "Other"),
        )
        assertEquals(HttpStatusCode.Conflict, duplicate.status)
        assertEquals("Email already registered", duplicate.error())
    }

    @Test
    fun `registered user can read update and login`() = testApplication {
        startApp()
        val email = uniqueEmail()
        val session = client.register(email = email, displayName = "  Padded Name  ")

        val profile = client.get("/api/v1/users/me") { bearerAuth(session.accessToken) }
        assertEquals(HttpStatusCode.OK, profile.status)
        assertEquals("Padded Name", profile.body().getValue("displayName").jsonPrimitive.content)
        assertEquals(email, profile.body().getValue("email").jsonPrimitive.content)

        val updated = client.put("/api/v1/users/me") {
            bearerAuth(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(json("displayName" to "Updated User"))
        }
        assertEquals("Updated User", updated.body().getValue("displayName").jsonPrimitive.content)

        val unchanged = client.put("/api/v1/users/me") {
            bearerAuth(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals("Updated User", unchanged.body().getValue("displayName").jsonPrimitive.content)

        val login = client.login(email.uppercase())
        assertEquals(HttpStatusCode.OK, login.status)
        assertEquals(session.userId, login.session().userId)
    }

    @Test
    fun `login failures do not reveal whether the account exists`() = testApplication {
        startApp()
        val email = uniqueEmail()
        client.register(email = email)

        val wrongPassword = client.login(email, "wrong-password-123")
        val unknownEmail = client.login(uniqueEmail())
        listOf(wrongPassword, unknownEmail).forEach { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid email or password", response.error())
        }
    }

    @Test
    fun `refresh rotates the token and rejects the old one`() = testApplication {
        startApp()
        val session = client.register()

        val rotated = client.refresh(session.refreshToken)
        assertEquals(HttpStatusCode.OK, rotated.status)
        val next = rotated.session()
        assertNotEquals(session.refreshToken, next.refreshToken)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/users/me") { bearerAuth(next.accessToken) }.status)

        val second = client.refresh(next.refreshToken)
        assertEquals(HttpStatusCode.OK, second.status)
    }

    @Test
    fun `reusing a rotated refresh token revokes every session of the user`() = testApplication {
        startApp()
        val email = uniqueEmail()
        val first = client.register(email = email)
        val otherDevice = client.login(email).session()
        val rotated = client.refresh(first.refreshToken).session()

        val reuse = client.refresh(first.refreshToken)
        assertEquals(HttpStatusCode.Unauthorized, reuse.status)
        assertEquals("Invalid or expired refresh token", reuse.error())

        assertEquals(HttpStatusCode.Unauthorized, client.refresh(rotated.refreshToken).status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(otherDevice.refreshToken).status)
        assertEquals(HttpStatusCode.OK, client.login(email).status)
    }

    @Test
    fun `refresh rejects invalid tokens`() = testApplication {
        startApp()
        val session = client.register()
        listOf("not-a-token", session.accessToken, "").forEach { token ->
            val response = client.refresh(token)
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Invalid or expired refresh token", response.error())
        }
    }

    @Test
    fun `logout revokes only the presented session`() = testApplication {
        startApp()
        val email = uniqueEmail()
        val phone = client.register(email = email)
        val tablet = client.login(email).session()

        val logout = client.logout(phone.refreshToken)
        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(HttpStatusCode.NoContent, client.logout("junk").status)

        assertEquals(HttpStatusCode.OK, client.refresh(tablet.refreshToken).status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(phone.refreshToken).status)
    }

    @Test
    fun `registration validates input`() = testApplication {
        startApp()
        val cases = mapOf(
            json("email" to "invalid", "password" to TEST_PASSWORD, "displayName" to "User") to
                "Invalid email address",
            json("email" to "${"a".repeat(250)}@example.com", "password" to TEST_PASSWORD, "displayName" to "User") to
                "Email must be at most 255 characters",
            json("email" to uniqueEmail(), "password" to "short", "displayName" to "User") to
                "Password must be at least 8 characters",
            json("email" to uniqueEmail(), "password" to "é".repeat(40), "displayName" to "User") to
                "Password must be at most 72 bytes",
            json("email" to uniqueEmail(), "password" to TEST_PASSWORD, "displayName" to "   ") to
                "Display name must not be blank",
            json("email" to uniqueEmail(), "password" to TEST_PASSWORD, "displayName" to "n".repeat(101)) to
                "Display name must be at most 100 characters",
        )
        cases.forEach { (body, message) ->
            val response = client.postJson("/api/v1/auth/register", body)
            assertEquals(HttpStatusCode.BadRequest, response.status, message)
            assertEquals(message, response.error())
        }
    }

    @Test
    fun `profile updates are validated`() = testApplication {
        startApp()
        val session = client.register()
        val response = client.put("/api/v1/users/me") {
            bearerAuth(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(json("displayName" to "x".repeat(101)))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Display name must be at most 100 characters", response.error())
    }

    @Test
    fun `account deletion cascades and keeps explicit 404 bodies`() = testApplication {
        val config = startApp()
        val email = uniqueEmail()
        val session = client.register(email = email)
        val task = """{"id":"${UUID.randomUUID()}","title":"Task","completed":false,"deleted":false,""" +
            """"updatedAt":1,"createdAt":1}"""
        val sync = client.postJson("/api/v1/tasks/sync", """{"tasks":[$task]}""") { bearerAuth(session.accessToken) }
        assertEquals(HttpStatusCode.OK, sync.status)
        assertEquals(1, config.countRows("tasks", session.userId))
        assertEquals(1, config.countRows("refresh_tokens", session.userId))

        val deletion = client.delete("/api/v1/users/me") { bearerAuth(session.accessToken) }
        assertEquals(HttpStatusCode.NoContent, deletion.status)
        assertEquals(0, config.countRows("users", session.userId))
        assertEquals(0, config.countRows("tasks", session.userId))
        assertEquals(0, config.countRows("refresh_tokens", session.userId))

        // The access token stays cryptographically valid until it expires; explicit bodies survive.
        val profile = client.get("/api/v1/users/me") { bearerAuth(session.accessToken) }
        assertEquals(HttpStatusCode.NotFound, profile.status)
        assertEquals("User not found", profile.error())
        val update = client.put("/api/v1/users/me") {
            bearerAuth(session.accessToken)
            contentType(ContentType.Application.Json)
            setBody(json("displayName" to "Ghost"))
        }
        assertEquals("User not found", update.error())
        assertEquals("User not found", client.delete("/api/v1/users/me") { bearerAuth(session.accessToken) }.error())
        val resync = client.postJson("/api/v1/tasks/sync", """{"tasks":[]}""") { bearerAuth(session.accessToken) }
        assertEquals(HttpStatusCode.NotFound, resync.status)
        assertEquals("User not found", resync.error())

        assertEquals(HttpStatusCode.Unauthorized, client.login(email).status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(session.refreshToken).status)
    }

    @Test
    fun `login and register share a strict per-client limit`() = testApplication {
        startApp(testConfig(rateLimit = RateLimitConfig(enabled = true, authRequestsPerMinute = 2)))
        val email = uniqueEmail()
        val session = client.register(email = email)
        assertEquals(HttpStatusCode.OK, client.login(email).status)

        val limited = client.login(email)
        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertEquals("Too many requests; retry later", limited.error())

        // Refresh has its own bucket.
        assertEquals(HttpStatusCode.OK, client.refresh(session.refreshToken).status)
    }

    @Test
    fun `rate limits are keyed by the proxy-reported client address`() = testApplication {
        startApp(testConfig(rateLimit = RateLimitConfig(enabled = true, authRequestsPerMinute = 1)))
        val email = uniqueEmail()
        client.register(email = email) // consumes the bucket of the direct (non-proxied) client

        suspend fun loginFrom(forwardedFor: String) = client.postJson(
            "/api/v1/auth/login",
            json("email" to email, "password" to TEST_PASSWORD),
        ) { header("X-Forwarded-For", forwardedFor) }

        assertEquals(HttpStatusCode.OK, loginFrom("203.0.113.10").status)
        assertEquals(HttpStatusCode.OK, loginFrom("203.0.113.11").status)
        assertEquals(HttpStatusCode.TooManyRequests, loginFrom("203.0.113.10").status)
        // A client-supplied first entry cannot escape the bucket of the proxy-appended address.
        assertEquals(HttpStatusCode.TooManyRequests, loginFrom("198.51.100.99, 203.0.113.10").status)
    }

    @Test
    fun `refresh and logout share a lenient limit`() = testApplication {
        startApp(testConfig(rateLimit = RateLimitConfig(enabled = true, refreshRequestsPerMinute = 2)))
        val session = client.register()
        assertEquals(HttpStatusCode.Unauthorized, client.refresh("junk").status)
        assertEquals(HttpStatusCode.NoContent, client.logout("junk").status)
        assertEquals(HttpStatusCode.TooManyRequests, client.refresh(session.refreshToken).status)
    }
}
