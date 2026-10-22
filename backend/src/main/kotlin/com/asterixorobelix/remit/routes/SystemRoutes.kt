package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.configuration.BuildInfo
import com.asterixorobelix.remit.dto.HealthResponse
import com.asterixorobelix.remit.dto.ServiceInfoResponse
import com.asterixorobelix.remit.dto.StatusResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

const val SERVICE_NAME = "remit"

fun Route.systemRoutes() {
    get("/") {
        call.respond(ServiceInfoResponse(service = SERVICE_NAME, version = BuildInfo.version))
    }

    // Liveness: the process is up.
    get("/health") {
        call.respond(
            HealthResponse(status = "healthy", timestamp = System.currentTimeMillis(), version = BuildInfo.version),
        )
    }

    get("/api/v1/status") {
        call.respond(StatusResponse(api = "running", version = BuildInfo.version))
    }
}
