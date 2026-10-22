package com.asterixorobelix.remit.configuration

/**
 * Application environment types.
 */
enum class Environment {
    /** Local development environment */
    DEVELOPMENT,

    /** Staging/QA environment */
    STAGING,

    /** Production environment */
    PRODUCTION,

    ;

    val isDevelopment: Boolean get() = this == DEVELOPMENT
    val isProduction: Boolean get() = this == PRODUCTION

    companion object {
        /**
         * Detects the environment from APP_ENVIRONMENT (or the ENVIRONMENT / KTOR_ENV aliases).
         *
         * Fails closed: when nothing is set the application runs with production rules, so a
         * forgotten variable can never expose development defaults such as Swagger UI or the
         * localhost CORS origins.
         * `./gradlew run` and docker-compose set APP_ENVIRONMENT=development explicitly.
         */
        fun current(env: EnvLookup = SystemEnv): Environment {
            val value = sequenceOf("APP_ENVIRONMENT", "ENVIRONMENT", "KTOR_ENV")
                .mapNotNull { name -> env(name)?.takeIf(String::isNotBlank) }
                .firstOrNull()
            return value?.let(::fromString) ?: PRODUCTION
        }

        /**
         * Parse environment from string value.
         * Supports various common naming conventions.
         */
        fun fromString(value: String): Environment {
            return when (value.uppercase().trim()) {
                "PRODUCTION", "PROD", "RELEASE" -> PRODUCTION

                "STAGING", "STAGE", "QA", "UAT" -> STAGING

                "DEVELOPMENT", "DEV", "LOCAL" -> DEVELOPMENT

                else -> throw IllegalArgumentException(
                    "Unsupported application environment: $value",
                )
            }
        }
    }
}
