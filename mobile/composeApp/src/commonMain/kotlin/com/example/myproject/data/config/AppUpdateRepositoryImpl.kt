package com.example.myproject.data.config

import com.example.myproject.data.network.ApiPaths
import com.example.myproject.data.network.bodyOrThrow
import com.example.myproject.domain.update.AppUpdateRepository
import com.example.myproject.domain.update.VersionPolicy
import com.example.myproject.util.runSuspendCatching
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
