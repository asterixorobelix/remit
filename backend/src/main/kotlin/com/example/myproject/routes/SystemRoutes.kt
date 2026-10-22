package com.example.myproject.routes

import com.example.myproject.configuration.BuildInfo
import com.example.myproject.dto.HealthResponse
import com.example.myproject.dto.ReadinessResponse
import com.example.myproject.dto.ServiceInfoResponse
import com.example.myproject.dto.StatusResponse
import com.example.myproject.plugins.isReady
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.v1.jdbc.Database

const val SERVICE_NAME = "myproject"

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
