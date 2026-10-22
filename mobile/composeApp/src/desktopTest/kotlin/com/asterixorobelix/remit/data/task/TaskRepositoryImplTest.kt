package com.asterixorobelix.remit.data.task

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.data.network.ApiJson
import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.domain.task.SyncStatus
import com.asterixorobelix.remit.domain.task.TaskSyncSettings
import com.asterixorobelix.remit.testing.TestAppDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Exercises the real SQLDelight schema (in-memory SQLite) against a fake sync endpoint. */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
    private val local = TaskLocalDataSource(AppDatabase(driver))

    /** Each element is one POST /tasks/sync request body. */
    private val sentBatches: MutableList<List<TaskDto>> = Collections.synchronizedList(mutableListOf())
    private val server = Collections.synchronizedMap(linkedMapOf<String, TaskDto>())
    private var requestReceived = CompletableDeferred<Unit>()
    private var releaseResponse: CompletableDeferred<Unit>? = null
    private var failWith: HttpStatusCode? = null

    private val client = HttpClient(
        MockEngine { request ->
            val batch = ApiJson.decodeFromString(TaskSyncRequest.serializer(), (request.body as TextContent).text)
            sentBatches += batch.tasks
            requestReceived.complete(Unit)
            releaseResponse?.await()
            failWith?.let { return@MockEngine respond("", it) }
            batch.tasks.forEach { incoming ->
                val existing = server[incoming.id]
                if (existing == null || incoming.updatedAt > existing.updatedAt) server[incoming.id] = incoming
            }
            val body = ApiJson.encodeToString(
                TaskSyncResponse.serializer(),
                TaskSyncResponse(server.values.toList(), 0),
            )
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        install(ContentNegotiation) { json(ApiJson) }
        defaultRequest {
            url("https://api.test")
            contentType(ContentType.Application.Json)
        }
    }

    @AfterTest
    fun tearDown() {
        client.close()
        driver.close()
    }

    private fun TestScope.repository(syncEnabled: Boolean = true) = TaskRepositoryImpl(
        local = local,
        remote = TaskRemoteDataSource(client),
        settings = TaskSyncSettings(syncEnabled),
        dispatchers = TestAppDispatchers(UnconfinedTestDispatcher(testScheduler)),
    )

    private suspend fun TaskRepositoryImpl.titles() = observeTasks().first().map { it.title }.sorted()

    @Test
    fun mutationsAreLocalFirstAndQueued() = runTest {
        val repository = repository()
        repository.create("A")
        val task = repository.observeTasks().first().single()
        repository.toggle(task)

        assertEquals(listOf("A"), repository.titles())
        assertEquals(SyncStatus.Pending(2), repository.syncStatus.value)
        assertTrue(sentBatches.isEmpty())
    }

    @Test
    fun syncSendsOnlyQueuedTasks() = runTest {
        val repository = repository()
        repository.create("A")
        repository.sync()
        repository.create("B")
        repository.sync()

        assertEquals(listOf(listOf("A"), listOf("B")), sentBatches.map { batch -> batch.map { it.title } })
        assertEquals(SyncStatus.Idle, repository.syncStatus.value)
        assertEquals(0, local.pendingCount())
    }

    @Test
    fun editsMadeDuringAnInFlightSyncAreKeptAndStayQueued() = runTest {
        val repository = repository()
        repository.create("A")
        releaseResponse = CompletableDeferred()
        val inFlight = async { repository.sync() }
        requestReceived.await()

        repository.create("B") // Written while the server is still processing A.
        releaseResponse?.complete(Unit)
        assertTrue(inFlight.await().isSuccess)

        assertEquals(listOf("A", "B"), repository.titles())
        assertEquals(1, local.pendingCount())

        releaseResponse = null
        repository.sync()
        assertEquals(listOf("B"), sentBatches.last().map { it.title })
        assertEquals(0, local.pendingCount())
    }

    @Test
    fun largeQueuesAreSentInBatchesWithinTheBackendLimit() = runTest {
        val repository = repository()
        repeat(MAX_SYNC_BATCH_SIZE + 1) { repository.create("Task $it") }

        assertTrue(repository.sync().isSuccess)

        assertEquals(listOf(MAX_SYNC_BATCH_SIZE, 1), sentBatches.map { it.size })
        assertEquals(MAX_SYNC_BATCH_SIZE + 1, repository.observeTasks().first().size)
        assertEquals(0, local.pendingCount())
    }

    @Test
    fun forcedSyncPullsServerChangesWithNothingQueued() = runTest {
        server["remote"] =
            TaskDto("remote", "From web", completed = false, deleted = false, updatedAt = 5, createdAt = 5)
        val repository = repository()

        repository.sync(force = false)
        assertTrue(sentBatches.isEmpty(), "nothing queued, no forced pull")

        repository.sync(force = true)
        assertEquals(listOf(emptyList()), sentBatches)
        assertEquals(listOf("From web"), repository.titles())
    }

    @Test
    fun deletesAreTombstonesThatReachTheServer() = runTest {
        val repository = repository()
        repository.create("A")
        repository.sync()
        repository.delete(repository.observeTasks().first().single())
        repository.sync()

        assertTrue(sentBatches.last().single().deleted)
        assertTrue(repository.titles().isEmpty())
    }

    @Test
    fun failuresKeepChangesQueuedWithBackoff() = runTest {
        val repository = repository()
        repository.create("A")
        failWith = HttpStatusCode.ServiceUnavailable

        val result = repository.sync()

        assertTrue(result.isFailure)
        assertEquals(SyncStatus.Error(DataError.SERVER, pendingCount = 1), repository.syncStatus.value)
        assertEquals(1, local.pendingCount())
        repository.sync(force = false)
        assertEquals(1, sentBatches.size, "operation is in backoff, automatic sync waits")

        failWith = null
        assertTrue(repository.sync(force = true).isSuccess)
        assertEquals(0, local.pendingCount())
    }

    @Test
    fun clearUserDataRemovesTasksAndQueue() = runTest {
        val repository = repository()
        repository.create("A")
        repository.clearUserData()
        assertTrue(repository.titles().isEmpty())
        assertEquals(0, local.pendingCount())
        assertIs<SyncStatus.Idle>(repository.syncStatus.value)
    }

    @Test
    fun localOnlyModeNeverQueuesOrCallsTheServer() = runTest {
        val repository = repository(syncEnabled = false)
        repository.create("A")
        assertTrue(repository.sync(force = true).isSuccess)
        assertEquals(0, local.pendingCount())
        assertTrue(sentBatches.isEmpty())
    }
}
