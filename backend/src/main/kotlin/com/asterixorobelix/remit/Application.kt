package com.asterixorobelix.remit

import com.asterixorobelix.remit.configuration.AppConfig
import com.asterixorobelix.remit.configuration.BuildInfo
import com.asterixorobelix.remit.plugins.configureDocumentation
import com.asterixorobelix.remit.plugins.configureHTTP
import com.asterixorobelix.remit.plugins.configureMonitoring
import com.asterixorobelix.remit.plugins.configureRouting
import com.asterixorobelix.remit.plugins.configureSerialization
import com.asterixorobelix.remit.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import java.util.TimeZone

private val logger = LoggerFactory.getLogger("com.asterixorobelix.remit.Application")

fun main() {
    // Run in UTC so server-side dates never depend on the host's time zone.
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val config = AppConfig.load()
    logger.info(
        "Starting v{} in {} environment on {}:{}",
        BuildInfo.version,
        config.environment.name.lowercase(),
        config.server.host,
        config.server.port,
    )

    // Ktor registers a JVM shutdown hook that stops the server with these grace settings.
    embeddedServer(
        factory = Netty,
        configure = {
            connector {
                host = config.server.host
                port = config.server.port
            }
            shutdownGracePeriod = config.server.shutdownGracePeriodMs
            shutdownTimeout = config.server.shutdownTimeoutMs
        },
        module = { module(config) },
    ).start(wait = true)
}

fun Application.module(config: AppConfig) {
    log.info("Swagger UI: {}", if (config.features.swaggerEnabled) "/swagger" else "disabled")
    monitor.subscribe(ApplicationStopping) { log.info("Shutting down") }

    configureMonitoring()
    configureSerialization(config.environment)
    configureHTTP(config.server, config.cors)
    configureStatusPages()
    configureDocumentation(config.features.swaggerEnabled)
    configureRouting(config)
}
