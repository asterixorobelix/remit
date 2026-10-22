package com.asterixorobelix.remit.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

const val SWAGGER_PATH = "swagger"

/**
 * Serves Swagger UI at `/swagger` and the static spec at `/swagger/documentation.yaml`, both from
 * `resources/openapi/documentation.yaml`. Nothing is generated or written at runtime.
 */
fun Application.configureDocumentation(enabled: Boolean) {
    if (!enabled) {
        log.info("Swagger UI disabled (SWAGGER_ENABLED=false)")
        return
    }
    routing {
        swaggerUI(path = SWAGGER_PATH, swaggerFile = "openapi/documentation.yaml")
    }
}
