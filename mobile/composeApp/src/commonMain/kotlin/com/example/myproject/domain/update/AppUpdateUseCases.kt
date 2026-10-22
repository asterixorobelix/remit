package com.example.myproject.domain.update

/** Version policy published by the backend (`GET /api/v1/config`). */
data class VersionPolicy(
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
)

interface AppUpdateRepository {
    suspend fun fetchVersionPolicy(): Result<VersionPolicy>
}

/**
 * True when this build must be updated before use: it is older than `minVersion`, or the server
 * forces the latest release and this build is older than `latestVersion`. Any failure (offline,
 * server down, malformed response) returns false so the app never locks users out by accident.
 */
class CheckForceUpdateUseCase(private val repository: AppUpdateRepository) {
    suspend operator fun invoke(currentVersion: String): Boolean {
        val policy = repository.fetchVersionPolicy().getOrNull() ?: return false
        return compareVersions(currentVersion, policy.minVersion) < 0 ||
            (policy.forceUpdate && compareVersions(currentVersion, policy.latestVersion) < 0)
    }
}

/**
 * Compares dotted numeric versions ("1.10.0" > "1.9.3"). Pre-release/build suffixes after `-` or
 * `+` are ignored and missing segments count as 0.
 */
fun compareVersions(left: String, right: String): Int {
    val a = left.numericSegments()
    val b = right.numericSegments()
    for (index in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(index) { 0 }.compareTo(b.getOrElse(index) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

private fun String.numericSegments(): List<Int> =
    trim().substringBefore('-').substringBefore('+').split('.').map { it.toIntOrNull() ?: 0 }
