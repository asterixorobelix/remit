package com.asterixorobelix.remit

import com.asterixorobelix.remit.configuration.AppConfig
import com.asterixorobelix.remit.configuration.CorsConfig
import com.asterixorobelix.remit.configuration.DatabaseConfig
import com.asterixorobelix.remit.configuration.Environment
import com.asterixorobelix.remit.configuration.FeatureFlags
import com.asterixorobelix.remit.configuration.JwtConfig
import com.asterixorobelix.remit.configuration.RateLimitConfig
import com.asterixorobelix.remit.configuration.RemoteConfig
import com.asterixorobelix.remit.configuration.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals

const val TEST_PASSWORD = "correct-horse-battery-staple"

/**
 * A hermetic configuration: never reads the process environment, so a developer's exported
 * DATABASE_URL or APP_ENVIRONMENT cannot leak into tests. Each call gets a private H2 database.
 */
fun testConfig(
    rateLimit: RateLimitConfig = RateLimitConfig(enabled = false),
    swaggerEnabled: Boolean = true,
    cors: CorsConfig = CorsConfig(),
    remoteConfig: RemoteConfig = RemoteConfig(),
    trustedProxyHops: Int = ServerConfig.DEFAULT_TRUSTED_PROXY_HOPS,
): AppConfig = AppConfig(
    environment = Environment.DEVELOPMENT,
    server = ServerConfig(trustedProxyHops = trustedProxyHops),
    database = DatabaseConfig.inMemoryH2("test-${UUID.randomUUID()}").copy(poolSize = 4),
    jwt = JwtConfig(
        secret = "test-only-secret-that-is-long-enough-for-hmac",
        issuer = "https://test.example.com",
        audience = "https://test.example.com",
    ),
    cors = cors,
    features = FeatureFlags(swaggerEnabled = swaggerEnabled),
    rateLimit = rateLimit,
    remoteConfig = remoteConfig,
    passwordHashCost = 4,
)

fun ApplicationTestBuilder.startApp(config: AppConfig = testConfig()): AppConfig {
    application { module(config) }
    return config
}

data class Session(val userId: Long, val accessToken: String, val refreshToken: String)

fun uniqueEmail(): String = "${UUID.randomUUID()}@example.com"

fun json(vararg fields: Pair<String, String>): String = buildJsonObject {
    fields.forEach { (key, value) -> put(key, value) }
}.toString()

suspend fun HttpClient.postJson(
    path: String,
    body: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = post(path) {
    contentType(ContentType.Application.Json)
    setBody(body)
    block()
}

suspend fun HttpClient.register(
    email: String = uniqueEmail(),
    password: String = TEST_PASSWORD,
    displayName: String = "Test User",
): Session {
    val response = postJson(
        "/api/v1/auth/register",
        json("email" to email, "password" to password, "displayName" to displayName),
    )
    assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
    return response.session()
}

suspend fun HttpClient.login(email: String, password: String = TEST_PASSWORD): HttpResponse =
    postJson("/api/v1/auth/login", json("email" to email, "password" to password))

suspend fun HttpClient.refresh(refreshToken: String): HttpResponse =
    postJson("/api/v1/auth/refresh", json("refreshToken" to refreshToken))

suspend fun HttpClient.logout(refreshToken: String): HttpResponse =
    postJson("/api/v1/auth/logout", json("refreshToken" to refreshToken))

suspend fun HttpResponse.body(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

suspend fun HttpResponse.error(): String = body().getValue("error").jsonPrimitive.content

suspend fun HttpResponse.session(): Session {
    val body = body()
    return Session(
        userId = body.getValue("user").jsonObject.getValue("id").jsonPrimitive.long,
        accessToken = body.getValue("accessToken").jsonPrimitive.content,
        refreshToken = body.getValue("refreshToken").jsonPrimitive.content,
    )
}

/** Counts rows directly in the test database (the in-memory H2 DB outlives the pool). */
fun AppConfig.countRows(table: String, userId: Long): Int {
    val ownerColumn = if (table == "users") "id" else "user_id"
    return DriverManager.getConnection(database.url, database.user, database.password).use { connection ->
        connection.prepareStatement("SELECT COUNT(*) FROM $table WHERE $ownerColumn = ?")
            .use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { result ->
                    result.next()
                    result.getInt(1)
                }
            }
    }
}
