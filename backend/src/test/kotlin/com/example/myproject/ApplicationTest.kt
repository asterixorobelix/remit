package com.example.myproject

import com.example.myproject.configuration.BuildInfo
import com.example.myproject.configuration.CorsConfig
import com.example.myproject.configuration.RemoteConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationTest {
    private val appVersion: String = Properties().apply {
        File("../app.properties").inputStream().use(::load)
    }.getProperty("VERSION_NAME")

    @Test
    fun `root returns service info`() = testApplication {
        startApp()
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("myproject", response.body().getValue("service").jsonPrimitive.content)
        assertEquals(appVersion, response.body().getValue("version").jsonPrimitive.content)
    }

    @Test
    fun `health reports the app properties version`() = testApplication {
        startApp()
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body()
        assertEquals("healthy", body.getValue("status").jsonPrimitive.content)
        assertTrue(body.getValue("timestamp").jsonPrimitive.long > 0)
        assertEquals(appVersion, body.getValue("version").jsonPrimitive.content)
        assertEquals(appVersion, BuildInfo.version)
    }

    @Test
    fun `readiness checks the database`() = testApplication {
        startApp()
        val response = client.get("/ready")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ready", response.body().getValue("status").jsonPrimitive.content)
    }

    @Test
    fun `api status reports the app properties version`() = testApplication {
        startApp()
        val body = client.get("/api/v1/status").body()
        assertEquals("running", body.getValue("api").jsonPrimitive.content)
        assertEquals(appVersion, body.getValue("version").jsonPrimitive.content)
    }

    @Test
    fun `remote config defaults to the build version`() = testApplication {
        startApp()
        val body = client.get("/api/v1/config").body()
        assertEquals(RemoteConfig.DEFAULT_MIN_VERSION, body.getValue("minVersion").jsonPrimitive.content)
        assertEquals(appVersion, body.getValue("latestVersion").jsonPrimitive.content)
        assertEquals("false", body.getValue("forceUpdate").jsonPrimitive.content)
        assertEquals("false", body.getValue("maintenanceMode").jsonPrimitive.content)
    }

    @Test
    fun `remote config serves configured values`() = testApplication {
        startApp(testConfig(remoteConfig = RemoteConfig("2.0.0", "2.1.0", forceUpdate = true, maintenanceMode = true)))
        val body = client.get("/api/v1/config").body()
        assertEquals("2.0.0", body.getValue("minVersion").jsonPrimitive.content)
        assertEquals("2.1.0", body.getValue("latestVersion").jsonPrimitive.content)
        assertEquals("true", body.getValue("forceUpdate").jsonPrimitive.content)
        assertEquals("true", body.getValue("maintenanceMode").jsonPrimitive.content)
    }

    @Test
    fun `a well-formed request id is echoed and others are replaced`() = testApplication {
        startApp()
        val provided = "client-req.id_12345"
        val echoed = client.get("/health") { header(HttpHeaders.XRequestId, provided) }
        assertEquals(provided, echoed.headers[HttpHeaders.XRequestId])

        val tooShort = client.get("/health") { header(HttpHeaders.XRequestId, "abc") }
        val illegal = client.get("/health") { header(HttpHeaders.XRequestId, "bad id with spaces<script>") }
        val missing = client.get("/health")
        listOf(tooShort, illegal, missing).forEach { response ->
            val generated = assertNotNull(response.headers[HttpHeaders.XRequestId])
            assertTrue(generated.matches(Regex("[0-9a-f-]{36}")), generated)
        }
        assertNotEquals(tooShort.headers[HttpHeaders.XRequestId], missing.headers[HttpHeaders.XRequestId])
    }

    @Test
    fun `swagger ui serves the static spec`() = testApplication {
        startApp()
        val ui = client.get("/swagger")
        assertEquals(HttpStatusCode.OK, ui.status)
        assertTrue(ui.bodyAsText().contains("swagger-ui"))

        val spec = client.get("/swagger/documentation.yaml")
        assertEquals(HttpStatusCode.OK, spec.status)
        val yaml = spec.bodyAsText()
        assertTrue(yaml.startsWith("openapi: 3.0.3"))
        listOf("/api/v1/auth/logout", "/api/v1/users/me", "/api/v1/tasks/sync", "bearerAuth").forEach {
            assertTrue(yaml.contains(it), "spec should document $it")
        }
    }

    @Test
    fun `swagger ui can be disabled`() = testApplication {
        startApp(testConfig(swaggerEnabled = false))
        assertEquals(HttpStatusCode.NotFound, client.get("/swagger").status)
    }

    @Test
    fun `security headers are set`() = testApplication {
        startApp()
        val response = client.get("/health")
        assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        assertEquals("DENY", response.headers["X-Frame-Options"])
    }

    @Test
    fun `cors allows configured origins and exposes the request id`() = testApplication {
        startApp(testConfig(cors = CorsConfig(allowedHosts = listOf("localhost:3000", "https://app.example.com"))))

        val preflight = client.options("/api/v1/users/me") {
            header(HttpHeaders.Origin, "https://app.example.com")
            header(HttpHeaders.AccessControlRequestMethod, "DELETE")
            header(HttpHeaders.AccessControlRequestHeaders, "authorization,content-type,x-request-id")
        }
        assertEquals(HttpStatusCode.OK, preflight.status)
        assertEquals("https://app.example.com", preflight.headers[HttpHeaders.AccessControlAllowOrigin])
        assertEquals(null, preflight.headers[HttpHeaders.AccessControlAllowCredentials])

        val simple = client.get("/health") { header(HttpHeaders.Origin, "http://localhost:3000") }
        assertEquals("http://localhost:3000", simple.headers[HttpHeaders.AccessControlAllowOrigin])
        assertTrue(simple.headers[HttpHeaders.AccessControlExposeHeaders].orEmpty().contains("X-Request-ID", true))

        val disallowed = client.get("/health") { header(HttpHeaders.Origin, "http://app.example.com") }
        assertEquals(null, disallowed.headers[HttpHeaders.AccessControlAllowOrigin])
    }
}
