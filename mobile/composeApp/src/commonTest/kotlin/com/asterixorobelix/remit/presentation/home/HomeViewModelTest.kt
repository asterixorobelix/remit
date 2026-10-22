package com.asterixorobelix.remit.presentation.home

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.home.DayPeriod
import com.asterixorobelix.remit.domain.home.GetDayPeriodUseCase
import com.asterixorobelix.remit.domain.session.GetCachedProfileUseCase
import com.asterixorobelix.remit.domain.session.UserProfile
import com.asterixorobelix.remit.domain.task.ObserveRecentTasksUseCase
import com.asterixorobelix.remit.domain.task.Task
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.testing.FakeAppConfig
import com.asterixorobelix.remit.testing.FakeProfileCache
import com.asterixorobelix.remit.testing.FakeTaskRepository
import com.asterixorobelix.remit.testing.awaitItemOfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val cache = FakeProfileCache(UserProfile("Ada", "ada@example.com"))

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun actionsAndRecentTasksFollowCapabilities() = runTest {
        val tasks = FakeTaskRepository().apply {
            this.tasks.value = listOf(Task("1", "Plan launch", completed = false, updatedAt = 1, createdAt = 1))
        }
        val viewModel = HomeViewModel(
            appConfig = FakeAppConfig(capabilities = Capability.entries.toSet()),
            getCachedProfile = GetCachedProfileUseCase(cache),
            getDayPeriod = GetDayPeriodUseCase(),
            observeRecentTasks = ObserveRecentTasksUseCase(tasks),
        )
        viewModel.uiState.test {
            val content = awaitItemOfType<HomeUiState.Content>()
            assertEquals("Ada", content.displayName)
            assertEquals(HomeAction.TASKS, content.featuredAction)
            assertEquals(HomeAction.entries.toSet(), content.quickActions.toSet())
            assertEquals(listOf("Plan launch"), content.recentTasks?.map { it.title })
        }
    }

    @Test
    fun minimalAppOnlyLinksToAlwaysAvailableScreens() = runTest {
        val viewModel = HomeViewModel(
            appConfig = FakeAppConfig(capabilities = emptySet()),
            getCachedProfile = GetCachedProfileUseCase(FakeProfileCache()),
            getDayPeriod = GetDayPeriodUseCase(),
            observeRecentTasks = null,
        )
        viewModel.uiState.test {
            val content = awaitItemOfType<HomeUiState.Content>()
            assertEquals(listOf(HomeAction.SEARCH, HomeAction.SETTINGS), content.quickActions)
            assertEquals(HomeAction.SEARCH, content.featuredAction)
            assertNull(content.recentTasks)
        }
    }

    @Test
    fun dayPeriodBoundaries() {
        val useCase = GetDayPeriodUseCase()
        assertEquals(DayPeriod.MORNING, useCase(hour = 11))
        assertEquals(DayPeriod.AFTERNOON, useCase(hour = 12))
        assertEquals(DayPeriod.EVENING, useCase(hour = 17))
    }
}
