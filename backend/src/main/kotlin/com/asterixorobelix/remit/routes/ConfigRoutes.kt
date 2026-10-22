package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.configuration.RemoteConfig
import com.asterixorobelix.remit.dto.AppConfigResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.configRoutes(remoteConfig: RemoteConfig) {
    val response = AppConfigResponse(
        minVersion = remoteConfig.minVersion,
        latestVersion = remoteConfig.latestVersion,
        forceUpdate = remoteConfig.forceUpdate,
        maintenanceMode = remoteConfig.maintenanceMode,
    )
    get("/api/v1/config") {
        call.respond(response)
    }
}
