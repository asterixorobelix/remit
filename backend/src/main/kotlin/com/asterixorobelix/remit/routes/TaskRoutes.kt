package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.dto.ErrorResponse
import com.asterixorobelix.remit.dto.TaskSyncRequest
import com.asterixorobelix.remit.dto.TaskSyncResponse
import com.asterixorobelix.remit.plugins.JWT_AUTH
import com.asterixorobelix.remit.plugins.userId
import com.asterixorobelix.remit.service.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.taskRoutes(taskService: TaskService) {
    authenticate(JWT_AUTH) {
        route("/api/v1/tasks") {
            get {
                call.respond(TaskSyncResponse(taskService.all(call.userId), System.currentTimeMillis()))
            }
            post("/sync") {
                val request = call.receive<TaskSyncRequest>()
                val tasks = taskService.sync(call.userId, request.tasks)
                if (tasks == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                } else {
                    call.respond(TaskSyncResponse(tasks, System.currentTimeMillis()))
                }
            }
        }
    }
}
