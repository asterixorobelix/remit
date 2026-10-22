package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.configuration.BuildInfo
import com.asterixorobelix.remit.dto.HealthResponse
import com.asterixorobelix.remit.dto.ReadinessResponse
import com.asterixorobelix.remit.dto.ServiceInfoResponse
import com.asterixorobelix.remit.dto.StatusResponse
import com.asterixorobelix.remit.plugins.isReady
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.v1.jdbc.Database

const val SERVICE_NAME = "remit"

fun Route.systemRoutes(database: Database) {
    get("/") {
        call.respond(ServiceInfoResponse(service = SERVICE_NAME, version = BuildInfo.version))
    }

    // Liveness: the process is up. Never touches the database.
    get("/health") {
        call.respond(
            HealthResponse(status = "healthy", timestamp = System.currentTimeMillis(), version = BuildInfo.version),
        )
    }

    // Readiness: the database answers queries.
    get("/ready") {
        if (database.isReady()) {
            call.respond(ReadinessResponse("ready"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, ReadinessResponse("not_ready"))
        }
    }

    get("/api/v1/status") {
        call.respond(StatusResponse(api = "running", version = BuildInfo.version))
    }
}
