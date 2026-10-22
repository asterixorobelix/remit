package com.example.myproject.plugins

import com.example.myproject.configuration.CorsConfig
import com.example.myproject.configuration.ServerConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.cors.CORSConfig
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

private const val HSTS_MAX_AGE_SECONDS = 31_536_000
private const val MAX_REQUEST_BODY_BYTES = 1024L * 1024L

fun Application.configureHTTP(server: ServerConfig, cors: CorsConfig) {
    // Resolve the client address from X-Forwarded-For. Taking the entry appended by the outermost
    // trusted proxy (rather than the first, client-controlled entry) prevents spoofing.
    if (server.trustedProxyHops > 0) {
        install(XForwardedHeaders) {
            skipLastProxies(server.trustedProxyHops - 1)
        }
    }

    install(RequestBodyLimit) {
        bodyLimit { MAX_REQUEST_BODY_BYTES }
    }

    install(CORS) {
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.XRequestId)
        exposeHeader(HttpHeaders.XRequestId)
        cors.allowedHosts.forEach(::allowOrigin)
        allowCredentials = cors.allowCredentials
    }

    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("Referrer-Policy", "no-referrer")
        header("Strict-Transport-Security", "max-age=$HSTS_MAX_AGE_SECONDS; includeSubDomains")
    }
}

/** `scheme://host[:port]` allows exactly that origin; a bare `host[:port]` allows http and https. */
private fun CORSConfig.allowOrigin(entry: String) {
    val schemeSeparator = entry.indexOf("://")
    if (schemeSeparator > 0) {
        allowHost(entry.substring(schemeSeparator + 3), schemes = listOf(entry.substring(0, schemeSeparator)))
    } else {
        allowHost(entry, schemes = listOf("http", "https"))
    }
}
