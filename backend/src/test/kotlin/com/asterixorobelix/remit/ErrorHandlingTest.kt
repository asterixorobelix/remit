package com.asterixorobelix.remit

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ErrorHandlingTest {
    @Test
    fun `malformed json is a 400 without internals`() = testApplication {
        startApp()
        val response = client.postJson("/api/v1/auth/login", """{"email": "a@example.com", "password": """)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed or missing request body", response.error())
        assertFalse(response.bodyAsText().contains("Exception"))
    }

    @Test
    fun `lenient json and missing fields are rejected`() = testApplication {
        startApp()
        val unquoted = client.postJson("/api/v1/auth/login", """{email: a@example.com, password: secret123}""")
        val missingField = client.postJson("/api/v1/auth/register", """{"email":"a@example.com"}""")
        val emptyObject = client.postJson("/api/v1/auth/refresh", "{}")
        mapOf("unquoted" to unquoted, "missingField" to missingField, "emptyObject" to emptyObject)
            .forEach { (case, response) ->
                assertEquals(HttpStatusCode.BadRequest, response.status, case)
                assertEquals("Malformed or missing request body", response.error())
            }

        // No body means no Content-Type either.
        val noBody = client.post("/api/v1/auth/refresh")
        assertEquals(HttpStatusCode.UnsupportedMediaType, noBody.status)
    }

    @Test
    fun `non json content is a 415`() = testApplication {
        startApp()
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Text.Plain)
            setBody("email=a@example.com")
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("Unsupported media type; send application/json", response.error())
    }

    @Test
    fun `oversized bodies are a 413`() = testApplication {
        startApp()
        val response = client.postJson("/api/v1/auth/login", "{\"email\":\"${"a".repeat(1_100_000)}\"}")
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("Request body too large", response.error())
    }

    @Test
    fun `unknown routes return the standard error body`() = testApplication {
        startApp()
        val response = client.get("/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Not found", response.error())
    }

    @Test
    fun `missing or invalid bearer tokens are a 401 with a body`() = testApplication {
        startApp()
        val session = client.register()
        val missing = client.get("/api/v1/users/me")
        val garbage = client.get("/api/v1/users/me") { bearerAuth("not-a-jwt") }
        val refreshAsAccess = client.get("/api/v1/users/me") { bearerAuth(session.refreshToken) }
        listOf(missing, garbage, refreshAsAccess).forEach { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("Authentication required", response.error())
            assertNotNull(response.headers[HttpHeaders.WWWAuthenticate])
        }
    }

    @Test
    fun `unexpected exceptions are a generic 500`() = testApplication {
        application {
            module(testConfig())
            routing {
                get("/test/boom") { error("secret internal detail") }
            }
        }
        val response = client.get("/test/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Internal server error", response.error())
        assertFalse(response.bodyAsText().contains("secret"))
    }
}
