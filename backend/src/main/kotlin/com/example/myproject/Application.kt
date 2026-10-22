package com.example.myproject

import com.example.myproject.configuration.AppConfig
import com.example.myproject.configuration.BuildInfo
import com.example.myproject.plugins.configureDatabases
import com.example.myproject.plugins.configureDocumentation
import com.example.myproject.plugins.configureHTTP
import com.example.myproject.plugins.configureMonitoring
import com.example.myproject.plugins.configureRouting
import com.example.myproject.plugins.configureSecurity
import com.example.myproject.plugins.configureSerialization
import com.example.myproject.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import java.util.TimeZone

private val logger = LoggerFactory.getLogger("com.example.myproject.Application")

fun main() {
    // TIMESTAMP columns are zone-less; always read and write them as UTC.
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
    log.info(
        "Database: {}; Swagger UI: {}; rate limiting: {}",
        if (config.database.isH2) "H2" else "PostgreSQL",
        if (config.features.swaggerEnabled) "/swagger" else "disabled",
        if (config.rateLimit.enabled) "enabled" else "disabled",
    )
    monitor.subscribe(ApplicationStopping) { log.info("Shutting down") }

    val database = configureDatabases(config.database)
    configureMonitoring()
    configureSerialization(config.environment)
    configureHTTP(config.server, config.cors)
    configureStatusPages()
    configureSecurity(config.jwt, config.rateLimit)
    configureDocumentation(config.features.swaggerEnabled)
    configureRouting(config, database)
}
