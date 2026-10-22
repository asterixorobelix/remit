package com.asterixorobelix.remit.data.config

import com.asterixorobelix.remit.data.network.ApiPaths
import com.asterixorobelix.remit.data.network.bodyOrThrow
import com.asterixorobelix.remit.domain.update.AppUpdateRepository
import com.asterixorobelix.remit.domain.update.VersionPolicy
import com.asterixorobelix.remit.util.runSuspendCatching
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
internal data class AppConfigResponse(
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val maintenanceMode: Boolean = false,
)

internal class AppUpdateRepositoryImpl(private val client: HttpClient) : AppUpdateRepository {
    override suspend fun fetchVersionPolicy(): Result<VersionPolicy> = runSuspendCatching {
        val response: AppConfigResponse = client.get(ApiPaths.APP_CONFIG).bodyOrThrow()
        VersionPolicy(response.minVersion, response.latestVersion, response.forceUpdate)
    }
}
