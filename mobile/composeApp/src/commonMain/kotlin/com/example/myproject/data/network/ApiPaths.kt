package com.example.myproject.data.network

/** Backend routes used by the app. Paths are resolved against the configured API base URL. */
internal object ApiPaths {
    const val AUTH_PREFIX = "/api/v1/auth/"
    const val LOGIN = "/api/v1/auth/login"
    const val REGISTER = "/api/v1/auth/register"
    const val REFRESH = "/api/v1/auth/refresh"
    const val LOGOUT = "/api/v1/auth/logout"
    const val CURRENT_USER = "/api/v1/users/me"
    const val TASKS_SYNC = "/api/v1/tasks/sync"
    const val APP_CONFIG = "/api/v1/config"
}
