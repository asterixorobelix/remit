package com.asterixorobelix.remit.platform

/**
 * The crash-reporting and analytics services a platform uses. Shared code sees only the
 * [CrashLogger] and [Analytics] interfaces. Each platform module binds one of these, and that is
 * the only place that names a provider (Crashlytics, Sentry, logcat): see
 * `androidObservabilityProviders`. `observabilityModule` wraps [crashLogger] in
 * [RedactingCrashLogger], so every provider gets redacted reports.
 */
class ObservabilityProviders(val crashLogger: CrashLogger, val analytics: Analytics)
