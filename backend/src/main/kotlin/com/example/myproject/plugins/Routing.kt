package com.example.myproject.plugins

import com.example.myproject.configuration.AppConfig
import com.example.myproject.routes.authRoutes
import com.example.myproject.routes.configRoutes
import com.example.myproject.routes.systemRoutes
import com.example.myproject.routes.taskRoutes
import com.example.myproject.routes.userRoutes
import com.example.myproject.service.AuthService
import com.example.myproject.service.TaskService
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
