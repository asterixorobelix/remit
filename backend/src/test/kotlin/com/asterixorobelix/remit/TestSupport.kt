package com.asterixorobelix.remit

import com.asterixorobelix.remit.configuration.AppConfig
import com.asterixorobelix.remit.configuration.CorsConfig
import com.asterixorobelix.remit.configuration.Environment
import com.asterixorobelix.remit.configuration.FeatureFlags
import com.asterixorobelix.remit.configuration.RemoteConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A hermetic configuration: never reads the process environment, so a developer's exported
 * APP_ENVIRONMENT or CORS settings cannot leak into tests.
 */
fun testConfig(
    swaggerEnabled: Boolean = true,
    cors: CorsConfig = CorsConfig(),
    remoteConfig: RemoteConfig = RemoteConfig(),
): AppConfig = AppConfig(
    environment = Environment.DEVELOPMENT,
    cors = cors,
    features = FeatureFlags(swaggerEnabled = swaggerEnabled),
    remoteConfig = remoteConfig,
)

fun ApplicationTestBuilder.startApp(config: AppConfig = testConfig()): AppConfig {
    application { module(config) }
    return config
}

suspend fun HttpClient.postJson(
    path: String,
    body: String,
    block: HttpRequestBuilder.() -> Unit = {},
): HttpResponse = post(path) {
    contentType(ContentType.Application.Json)
    setBody(body)
    block()
}

suspend fun HttpResponse.body(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

suspend fun HttpResponse.error(): String = body().getValue("error").jsonPrimitive.content
