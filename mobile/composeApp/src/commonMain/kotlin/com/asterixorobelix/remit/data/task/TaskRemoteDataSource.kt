package com.asterixorobelix.remit.data.task

import com.asterixorobelix.remit.data.network.ApiPaths
import com.asterixorobelix.remit.data.network.bodyOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class TaskRemoteDataSource(private val client: HttpClient) {
    suspend fun sync(tasks: List<TaskDto>): TaskSyncResponse =
        client.post(ApiPaths.TASKS_SYNC) { setBody(TaskSyncRequest(tasks)) }.bodyOrThrow()
}
