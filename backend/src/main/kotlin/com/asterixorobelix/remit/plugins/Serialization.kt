package com.asterixorobelix.remit.plugins

import com.asterixorobelix.remit.configuration.Environment
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.configureSerialization(environment: Environment) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = environment.isDevelopment
                isLenient = false
                ignoreUnknownKeys = true
                encodeDefaults = true
            },
        )
    }
}
