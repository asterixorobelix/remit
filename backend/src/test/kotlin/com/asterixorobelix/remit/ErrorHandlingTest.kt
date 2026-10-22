package com.asterixorobelix.remit

import com.asterixorobelix.remit.dto.ErrorResponse
import com.asterixorobelix.remit.service.ValidationException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Serializable
private data class EchoRequest(val name: String)

/**
 * Starts the application with test-only routes that read a JSON body, reject input, answer with
 * an explicit error body, and fail unexpectedly, so the error mapping is exercised end to end.
 */
private fun ApplicationTestBuilder.startAppWithTestRoutes() {
    application {
        module(testConfig())
        routing {
            post("/test/echo") { call.respond(call.receive<EchoRequest>()) }
            get("/test/invalid") { throw ValidationException("Name must not be blank") }
            get("/test/missing") { call.respond(HttpStatusCode.NotFound, ErrorResponse("Item not found")) }
            get("/test/boom") { error("secret internal detail") }
        }
    }
}

class ErrorHandlingTest {
    @Test
    fun `a well-formed json body is accepted`() = testApplication {
        startAppWithTestRoutes()
        val response = client.postJson("/test/echo", """{"name": "Ada"}""")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Ada", response.body().getValue("name").jsonPrimitive.content)
    }

    @Test
    fun `malformed json is a 400 without internals`() = testApplication {
        startAppWithTestRoutes()
        val response = client.postJson("/test/echo", """{"name": """)
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed or missing request body", response.error())
        assertFalse(response.bodyAsText().contains("Exception"))
    }

    @Test
    fun `lenient json and missing fields are rejected`() = testApplication {
        startAppWithTestRoutes()
        val unquoted = client.postJson("/test/echo", """{name: Ada}""")
        val missingField = client.postJson("/test/echo", "{}")
        mapOf("unquoted" to unquoted, "missingField" to missingField).forEach { (case, response) ->
            assertEquals(HttpStatusCode.BadRequest, response.status, case)
            assertEquals("Malformed or missing request body", response.error())
        }

        // No body means no Content-Type either.
        val noBody = client.post("/test/echo")
        assertEquals(HttpStatusCode.UnsupportedMediaType, noBody.status)
    }

    @Test
    fun `non json content is a 415`() = testApplication {
        startAppWithTestRoutes()
        val response = client.post("/test/echo") {
            contentType(ContentType.Text.Plain)
            setBody("name=Ada")
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertEquals("Unsupported media type; send application/json", response.error())
    }

    @Test
    fun `oversized bodies are a 413`() = testApplication {
        startAppWithTestRoutes()
        val response = client.postJson("/test/echo", "{\"name\":\"${"a".repeat(1_100_000)}\"}")
        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertEquals("Request body too large", response.error())
    }

    @Test
    fun `validation failures are a 400 with their message`() = testApplication {
        startAppWithTestRoutes()
        val response = client.get("/test/invalid")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Name must not be blank", response.error())
    }

    @Test
    fun `unknown routes return the standard error body`() = testApplication {
        startApp()
        val response = client.get("/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Not found", response.error())
    }

    @Test
    fun `explicit error bodies are preserved`() = testApplication {
        startAppWithTestRoutes()
        val response = client.get("/test/missing")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Item not found", response.error())
    }

    @Test
    fun `unexpected exceptions are a generic 500`() = testApplication {
        startAppWithTestRoutes()
        val response = client.get("/test/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals("Internal server error", response.error())
        assertFalse(response.bodyAsText().contains("secret"))
    }
}
