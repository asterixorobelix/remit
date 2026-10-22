package com.example.myproject.domain.task

import app.cash.turbine.test
import com.example.myproject.testing.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskUseCasesTest {
    private val repository = FakeTaskRepository()

    @Test
    fun createTrimsTitleAndUpdatesLocalFlow() = runTest {
        val create = CreateTaskUseCase(repository)
        repository.observeTasks().test {
            assertEquals(emptyList(), awaitItem())
            assertTrue(create("  Ship app  "))
            assertEquals("Ship app", awaitItem().single().title)
        }
    }

    @Test
    fun createRejectsBlankTitles() = runTest {
        assertFalse(CreateTaskUseCase(repository)("   "))
        assertTrue(repository.created.isEmpty())
    }

    @Test
    fun createCapsTitleAtBackendLimit() = runTest {
        CreateTaskUseCase(repository)("x".repeat(TASK_TITLE_MAX_LENGTH + 50))
        assertEquals(TASK_TITLE_MAX_LENGTH, repository.created.single().length)
    }

    @Test
    fun syncFailureIsReturned() = runTest {
        repository.syncFailure = com.example.myproject.domain.common.DataError.NETWORK
        assertTrue(SyncTasksUseCase(repository)(force = true).isFailure)
        assertEquals(listOf(true), repository.syncCalls)
    }
}
