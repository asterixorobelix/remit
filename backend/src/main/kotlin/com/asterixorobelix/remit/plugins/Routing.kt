package com.asterixorobelix.remit.plugins

import com.asterixorobelix.remit.configuration.AppConfig
import com.asterixorobelix.remit.routes.PaymentsFeature
import com.asterixorobelix.remit.routes.configRoutes
import com.asterixorobelix.remit.routes.paymentRoutes
import com.asterixorobelix.remit.routes.systemRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(config: AppConfig, payments: PaymentsFeature?) {
    routing {
        systemRoutes()
        configRoutes(config.remoteConfig)
        paymentRoutes(payments)
    }
}
