package com.example.myproject.configuration

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Reads a configuration value by name; [SystemEnv] in production, a map in tests. */
typealias EnvLookup = (String) -> String?

val SystemEnv: EnvLookup = System::getenv

/**
 * Application configuration loaded from environment variables.
 *
 * Nothing loads `.env` files automatically: export variables in the shell or set them on the
 * deployment platform. See `backend/.env.example` for every supported variable.
 */
data class AppConfig(
    val environment: Environment,
    val server: ServerConfig = ServerConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val jwt: JwtConfig = JwtConfig(),
    val cors: CorsConfig = CorsConfig(),
    val features: FeatureFlags = FeatureFlags(),
    val rateLimit: RateLimitConfig = RateLimitConfig(),
    val remoteConfig: RemoteConfig = RemoteConfig(),
    /** BCrypt cost factor. Not configurable from the environment; tests lower it for speed. */
    val passwordHashCost: Int = DEFAULT_PASSWORD_HASH_COST,
) {
    companion object {
        const val DEFAULT_PASSWORD_HASH_COST = 12

        /**
         * Loads configuration from [env] and validates it. Throws [IllegalStateException] when a
         * non-development environment is missing required production settings.
         */
        fun load(env: EnvLookup = SystemEnv): AppConfig {
            val environment = Environment.current(env)
            return AppConfig(
                environment = environment,
                server = ServerConfig.fromEnv(env),
                database = DatabaseConfig.fromEnv(env),
                jwt = JwtConfig.fromEnv(env),
                cors = CorsConfig.fromEnv(env, environment),
                features = FeatureFlags.fromEnv(env, environment),
                rateLimit = RateLimitConfig.fromEnv(env),
                remoteConfig = RemoteConfig.fromEnv(env),
            ).also(AppConfig::validate)
        }
    }

    /** Problems that make this configuration unsafe to run outside development. */
    fun validationErrors(): List<String> {
        if (environment.isDevelopment) return emptyList()
        return buildList {
            if (!database.isPostgres) add("DATABASE_URL must point to PostgreSQL")
            addAll(jwt.productionErrors())
        }
    }

    fun validate() {
        val errors = validationErrors()
        check(errors.isEmpty()) {
            "Invalid ${environment.name.lowercase()} configuration: ${errors.joinToString("; ")}. " +
                "For local development set APP_ENVIRONMENT=development."
        }
    }
}

/**
 * Server configuration.
 *
 * [trustedProxyHops] is the number of reverse proxies in front of the server that append to
 * `X-Forwarded-For`. The client address is taken from that position so it cannot be spoofed.
 * Use 0 when the server is exposed directly.
 */
data class ServerConfig(
    val port: Int = DEFAULT_PORT,
    val host: String = DEFAULT_HOST,
    val shutdownGracePeriodMs: Long = DEFAULT_SHUTDOWN_PERIOD,
    val shutdownTimeoutMs: Long = DEFAULT_SHUTDOWN_TIMEOUT,
    val trustedProxyHops: Int = DEFAULT_TRUSTED_PROXY_HOPS,
) {
    companion object {
        const val DEFAULT_PORT = 8085
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_SHUTDOWN_PERIOD = 1000L
        const val DEFAULT_SHUTDOWN_TIMEOUT = 5000L
        const val DEFAULT_TRUSTED_PROXY_HOPS = 1

        fun fromEnv(env: EnvLookup) = ServerConfig(
            port = env.int("PORT", DEFAULT_PORT),
            host = env.string("HOST", DEFAULT_HOST),
            shutdownGracePeriodMs = env.long("SHUTDOWN_GRACE_PERIOD_MS", DEFAULT_SHUTDOWN_PERIOD),
            shutdownTimeoutMs = env.long("SHUTDOWN_TIMEOUT_MS", DEFAULT_SHUTDOWN_TIMEOUT),
            trustedProxyHops = env.int("TRUSTED_PROXY_HOPS", DEFAULT_TRUSTED_PROXY_HOPS).coerceAtLeast(0),
        )
    }
}

/**
 * Database configuration. H2 URLs always run in PostgreSQL compatibility mode so that local
 * development and tests exercise the same SQL as production.
 */
data class DatabaseConfig(
    val url: String = DEFAULT_URL,
    val driver: String = H2_DRIVER,
    val user: String = H2_USER,
    val password: String = "",
    val poolSize: Int = DEFAULT_POOL_SIZE,
    val connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT,
    val idleTimeoutMs: Long = DEFAULT_IDLE_TIMEOUT,
    val maxLifetimeMs: Long = DEFAULT_MAX_LIFETIME,
    val runMigrations: Boolean = true,
) {
    val isH2: Boolean get() = url.startsWith("jdbc:h2:")
    val isPostgres: Boolean get() = url.startsWith("jdbc:postgresql:")

    companion object {
        const val H2_COMPATIBILITY_OPTIONS = "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
        const val DEFAULT_URL = "jdbc:h2:mem:myproject;$H2_COMPATIBILITY_OPTIONS;DB_CLOSE_DELAY=-1"
        const val H2_DRIVER = "org.h2.Driver"
        const val H2_USER = "sa"
        const val POSTGRES_DRIVER = "org.postgresql.Driver"
        const val DEFAULT_POOL_SIZE = 10
        const val DEFAULT_CONNECTION_TIMEOUT = 30_000L
        const val DEFAULT_IDLE_TIMEOUT = 600_000L
        const val DEFAULT_MAX_LIFETIME = 1_800_000L

        /** A private in-memory H2 database (used by tests). */
        fun inMemoryH2(name: String) = DatabaseConfig(
            url = "jdbc:h2:mem:$name;$H2_COMPATIBILITY_OPTIONS;DB_CLOSE_DELAY=-1",
        )

        fun fromEnv(env: EnvLookup): DatabaseConfig {
            val rawDatabaseUrl = env.string("DATABASE_URL", DEFAULT_URL)
            val databaseUrl = normalizeDatabaseUrl(rawDatabaseUrl)
            val isPostgres = databaseUrl.startsWith("jdbc:postgresql:")
            val uriCredentials = providerUrlCredentials(rawDatabaseUrl)

            return DatabaseConfig(
                url = databaseUrl,
                driver = if (isPostgres) POSTGRES_DRIVER else H2_DRIVER,
                user = if (isPostgres) env("DATABASE_USER") ?: uriCredentials.first else H2_USER,
                password = if (isPostgres) env("DATABASE_PASSWORD") ?: uriCredentials.second else "",
                poolSize = env.int("DB_POOL_SIZE", DEFAULT_POOL_SIZE),
                connectionTimeoutMs = env.long("DB_CONNECTION_TIMEOUT_MS", DEFAULT_CONNECTION_TIMEOUT),
                idleTimeoutMs = env.long("DB_IDLE_TIMEOUT_MS", DEFAULT_IDLE_TIMEOUT),
                maxLifetimeMs = env.long("DB_MAX_LIFETIME_MS", DEFAULT_MAX_LIFETIME),
                runMigrations = env.boolean("RUN_MIGRATIONS", true),
            )
        }

        /**
         * Converts provider URLs (`postgres://user:pass@host:port/db`) to JDBC and makes sure H2
         * URLs use PostgreSQL compatibility mode.
         */
        internal fun normalizeDatabaseUrl(url: String): String = when {
            isProviderPostgresUrl(url) -> {
                val uri = URI(url)
                val authority = buildString {
                    append(uri.host ?: error("DATABASE_URL must include a host"))
                    if (uri.port != -1) append(":${uri.port}")
                }
                val query = uri.rawQuery?.let { "?$it" }.orEmpty()
                "jdbc:postgresql://$authority${uri.rawPath}$query"
            }

            url.startsWith("jdbc:h2:") && !url.contains(";MODE=", ignoreCase = true) ->
                "$url;$H2_COMPATIBILITY_OPTIONS"

            else -> url
        }

        private fun isProviderPostgresUrl(url: String) =
            url.startsWith("postgresql://") || url.startsWith("postgres://")

        private fun providerUrlCredentials(url: String): Pair<String, String> {
            if (!isProviderPostgresUrl(url)) return "" to ""
            val parts = URI(url).userInfo
                ?.split(":", limit = 2)
                ?.map { URLDecoder.decode(it, StandardCharsets.UTF_8) }
                .orEmpty()
            return parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
        }
    }
}

/**
 * JWT authentication configuration.
 */
data class JwtConfig(
    val secret: String = DEFAULT_SECRET,
    val issuer: String = DEFAULT_ISSUER,
    val audience: String = DEFAULT_AUDIENCE,
    val realm: String = DEFAULT_REALM,
    val expirationMs: Long = DEFAULT_EXPIRATION_MS,
    val refreshExpirationMs: Long = DEFAULT_REFRESH_EXPIRATION_MS,
) {
    /** Reasons this configuration must not be used outside development. */
    fun productionErrors(): List<String> = buildList {
        if (secret.length < MIN_SECRET_LENGTH) {
            add("JWT_SECRET must contain at least $MIN_SECRET_LENGTH characters")
        }
        if (secret.contains(PLACEHOLDER_MARKER, ignoreCase = true)) {
            add("JWT_SECRET must be a generated secret, not a placeholder (openssl rand -base64 48)")
        }
        if (issuer.contains("localhost") || audience.contains("localhost")) {
            add("JWT_ISSUER and JWT_AUDIENCE must not use localhost")
        }
    }

    companion object {
        /** Development-only default. Rejected outside development. */
        const val DEFAULT_SECRET = "change-me-in-production"
        const val DEFAULT_ISSUER = "http://localhost:8085"
        const val DEFAULT_AUDIENCE = "http://localhost:8085"
        const val DEFAULT_REALM = "Access"
        const val DEFAULT_EXPIRATION_MS = 3_600_000L // 1 hour
        const val DEFAULT_REFRESH_EXPIRATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
        const val MIN_SECRET_LENGTH = 32

        /** Matches the default secret and "CHANGE_ME"-style placeholders in any case. */
        private const val PLACEHOLDER_MARKER = "change"

        fun fromEnv(env: EnvLookup) = JwtConfig(
            secret = env.string("JWT_SECRET", DEFAULT_SECRET),
            issuer = env.string("JWT_ISSUER", DEFAULT_ISSUER),
            audience = env.string("JWT_AUDIENCE", DEFAULT_AUDIENCE),
            realm = env.string("JWT_REALM", DEFAULT_REALM),
            expirationMs = env.long("JWT_EXPIRATION_MS", DEFAULT_EXPIRATION_MS),
            refreshExpirationMs = env.long("JWT_REFRESH_EXPIRATION_MS", DEFAULT_REFRESH_EXPIRATION_MS),
        )
    }
}

/**
 * CORS configuration for browser clients. Mobile apps do not need CORS.
 *
 * Entries are `host[:port]` (http and https allowed) or `scheme://host[:port]`.
 */
data class CorsConfig(
    val allowedHosts: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
) {
    companion object {
        val DEVELOPMENT_HOSTS = listOf(
            "localhost:3000",
            "localhost:5173",
            "127.0.0.1:3000",
            "127.0.0.1:5173",
        )

        fun fromEnv(env: EnvLookup, environment: Environment): CorsConfig {
            val allowedHosts = env("CORS_ALLOWED_HOSTS")
                ?.split(",")
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?: if (environment.isDevelopment) DEVELOPMENT_HOSTS else emptyList()
            return CorsConfig(
                allowedHosts = allowedHosts,
                allowCredentials = env.boolean("CORS_ALLOW_CREDENTIALS", false),
            )
        }
    }
}

/**
 * Feature flags for conditional functionality.
 */
data class FeatureFlags(
    val swaggerEnabled: Boolean = true,
) {
    companion object {
        fun fromEnv(env: EnvLookup, environment: Environment) = FeatureFlags(
            swaggerEnabled = env.boolean("SWAGGER_ENABLED", !environment.isProduction),
        )
    }
}

/**
 * Per-client rate limits. Login/register share a strict bucket; refresh/logout use a lenient one.
 */
data class RateLimitConfig(
    val enabled: Boolean = true,
    val authRequestsPerMinute: Int = DEFAULT_AUTH_PER_MINUTE,
    val refreshRequestsPerMinute: Int = DEFAULT_REFRESH_PER_MINUTE,
) {
    companion object {
        const val DEFAULT_AUTH_PER_MINUTE = 10
        const val DEFAULT_REFRESH_PER_MINUTE = 30

        fun fromEnv(env: EnvLookup) = RateLimitConfig(
            enabled = env.boolean("RATE_LIMITING_ENABLED", true),
            authRequestsPerMinute = env.int("RATE_LIMIT_AUTH_PER_MINUTE", DEFAULT_AUTH_PER_MINUTE),
            refreshRequestsPerMinute = env.int("RATE_LIMIT_REFRESH_PER_MINUTE", DEFAULT_REFRESH_PER_MINUTE),
        )
    }
}

/**
 * Values served by `GET /api/v1/config` to mobile clients.
 */
data class RemoteConfig(
    val minVersion: String = DEFAULT_MIN_VERSION,
    val latestVersion: String = BuildInfo.version,
    val forceUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_MIN_VERSION = "0.0.0"

        fun fromEnv(env: EnvLookup) = RemoteConfig(
            minVersion = env.string("MIN_APP_VERSION", DEFAULT_MIN_VERSION),
            latestVersion = env.string("LATEST_APP_VERSION", BuildInfo.version),
            forceUpdate = env.boolean("FORCE_UPDATE", false),
            maintenanceMode = env.boolean("MAINTENANCE_MODE", false),
        )
    }
}

private fun EnvLookup.string(name: String, default: String): String =
    this(name)?.takeIf(String::isNotBlank) ?: default

private fun EnvLookup.int(name: String, default: Int): Int = this(name)?.trim()?.toIntOrNull() ?: default

private fun EnvLookup.long(name: String, default: Long): Long = this(name)?.trim()?.toLongOrNull() ?: default

private fun EnvLookup.boolean(name: String, default: Boolean): Boolean =
    this(name)?.trim()?.lowercase()?.toBooleanStrictOrNull() ?: default
