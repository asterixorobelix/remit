package com.asterixorobelix.remit.plugins

import com.asterixorobelix.remit.configuration.AppConfig
import com.asterixorobelix.remit.routes.authRoutes
import com.asterixorobelix.remit.routes.configRoutes
import com.asterixorobelix.remit.routes.systemRoutes
import com.asterixorobelix.remit.routes.taskRoutes
import com.asterixorobelix.remit.routes.userRoutes
import com.asterixorobelix.remit.service.AuthService
import com.asterixorobelix.remit.service.TaskService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureRouting(config: AppConfig, database: Database) {
    val authService = AuthService(database, config.jwt, config.passwordHashCost)
    val taskService = TaskService(database)

    routing {
        systemRoutes(database)
        configRoutes(config.remoteConfig)
        authRoutes(authService, rateLimitingEnabled = config.rateLimit.enabled)
        userRoutes(authService)
        taskRoutes(taskService)
    }
}
