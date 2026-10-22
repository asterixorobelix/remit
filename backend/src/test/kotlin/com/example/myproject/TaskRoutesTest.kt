package com.example.myproject

import com.example.myproject.service.TaskService
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRoutesTest {
    private data class TaskInput(
        val id: String = UUID.randomUUID().toString(),
        val title: String = "Task",
        val updatedAt: Long = 10,
        val createdAt: Long = 1,
        val deleted: Boolean = false,
    )

    private suspend fun HttpClient.sync(token: String, vararg tasks: TaskInput): HttpResponse {
        val body = buildJsonObject {
            put(
                "tasks",
                buildJsonArray {
                    tasks.forEach { task ->
                        add(
                            buildJsonObject {
                                put("id", task.id)
                                put("title", task.title)
                                put("completed", false)
                                put("deleted", task.deleted)
                                put("updatedAt", task.updatedAt)
                                put("createdAt", task.createdAt)
                            },
                        )
                    }
                },
            )
        }
        return postJson("/api/v1/tasks/sync", body.toString()) { bearerAuth(token) }
    }

    private suspend fun HttpResponse.tasks(): List<JsonObject> =
        body().getValue("tasks").jsonArray.map { it.jsonObject }

    private suspend fun HttpClient.storedTasks(token: String): List<JsonObject> =
        get("/api/v1/tasks") { bearerAuth(token) }.tasks()

    private fun JsonObject.title() = getValue("title").jsonPrimitive.content

    @Test
    fun `task endpoints require authentication`() = testApplication {
        startApp()
        val response = client.postJson("/api/v1/tasks/sync", """{"tasks":[]}""")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/tasks").status)
    }

    @Test
    fun `last write wins in both directions`() = testApplication {
        startApp()
        val token = client.register().accessToken
        val id = UUID.randomUUID().toString()

        assertEquals("v20", client.sync(token, TaskInput(id, "v20", updatedAt = 20)).tasks().single().title())
        assertEquals("v20", client.sync(token, TaskInput(id, "older", updatedAt = 10)).tasks().single().title())
        assertEquals("v20", client.sync(token, TaskInput(id, "same time", updatedAt = 20)).tasks().single().title())

        val newer = client.sync(token, TaskInput(id, "  v30  ", updatedAt = 30, deleted = true)).tasks().single()
        assertEquals("v30", newer.title())
        assertEquals(30, newer.getValue("updatedAt").jsonPrimitive.long)
        assertEquals(1, newer.getValue("createdAt").jsonPrimitive.long)
        assertTrue(newer.getValue("deleted").jsonPrimitive.boolean)

        assertEquals("v30", client.storedTasks(token).single().title())
    }

    @Test
    fun `task ids are scoped to their owner`() = testApplication {
        startApp()
        val alice = client.register().accessToken
        val bob = client.register().accessToken
        val sharedId = UUID.randomUUID().toString()

        assertEquals(HttpStatusCode.OK, client.sync(alice, TaskInput(sharedId, "alice", updatedAt = 10)).status)
        val bobSync = client.sync(bob, TaskInput(sharedId, "bob", updatedAt = 5), TaskInput(title = "bob 2"))
        assertEquals(HttpStatusCode.OK, bobSync.status)

        assertEquals(listOf("alice"), client.storedTasks(alice).map { it.title() })
        assertEquals(setOf("bob", "bob 2"), client.storedTasks(bob).map { it.title() }.toSet())
    }

    @Test
    fun `concurrent syncs of the same new task converge on the newest write`() = testApplication {
        startApp()
        val token = client.register().accessToken
        val id = UUID.randomUUID().toString()

        val responses = coroutineScope {
            (1..8).map { version ->
                async { client.sync(token, TaskInput(id, "v$version", updatedAt = version * 10L)) }
            }.awaitAll()
        }
        responses.forEach { assertEquals(HttpStatusCode.OK, it.status) }
        assertEquals(listOf("v8"), client.storedTasks(token).map { it.title() })
    }

    @Test
    fun `future timestamps are clamped to the server clock`() = testApplication {
        startApp()
        val token = client.register().accessToken
        val farFuture = System.currentTimeMillis() + 24 * 60 * 60 * 1000L

        val stored = client.sync(token, TaskInput(updatedAt = farFuture, createdAt = farFuture)).tasks().single()
        val latestAllowed = System.currentTimeMillis() + TaskService.MAX_CLOCK_SKEW.toMillis()
        assertTrue(stored.getValue("updatedAt").jsonPrimitive.long <= latestAllowed)
        assertTrue(stored.getValue("createdAt").jsonPrimitive.long <= latestAllowed)
    }

    @Test
    fun `invalid tasks reject the whole batch`() = testApplication {
        startApp()
        val token = client.register().accessToken
        val cases = mapOf(
            TaskInput(id = "not-a-uuid") to "Task at index 1: id must be a UUID",
            TaskInput(id = "x".repeat(37)) to "Task at index 1: id must be a UUID",
            TaskInput(title = "   ") to "Task at index 1: title must not be blank",
            TaskInput(title = "t".repeat(241)) to "Task at index 1: title must be at most 240 characters",
            TaskInput(updatedAt = -1) to "Task at index 1: timestamps must be non-negative epoch milliseconds",
        )
        cases.forEach { (invalid, message) ->
            val response = client.sync(token, TaskInput(), invalid)
            assertEquals(HttpStatusCode.BadRequest, response.status, message)
            assertEquals(message, response.error())
        }
        assertEquals(emptyList(), client.storedTasks(token))
    }

    @Test
    fun `sync batches are capped`() = testApplication {
        startApp()
        val token = client.register().accessToken
        val maximum = List(TaskService.MAX_SYNC_BATCH_SIZE) { TaskInput(title = "t$it") }

        val tooMany = client.sync(token, *(maximum + TaskInput()).toTypedArray())
        assertEquals(HttpStatusCode.BadRequest, tooMany.status)
        assertEquals("A sync batch may contain at most 500 tasks", tooMany.error())

        val accepted = client.sync(token, *maximum.toTypedArray())
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertEquals(TaskService.MAX_SYNC_BATCH_SIZE, accepted.tasks().size)
    }
}
