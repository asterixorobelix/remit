package com.example.myproject.presentation.home

import app.cash.turbine.test
import com.example.myproject.domain.home.DayPeriod
import com.example.myproject.domain.home.GetDayPeriodUseCase
import com.example.myproject.domain.session.GetCachedProfileUseCase
import com.example.myproject.domain.session.UserProfile
import com.example.myproject.domain.task.ObserveRecentTasksUseCase
import com.example.myproject.domain.task.Task
import com.example.myproject.platform.Capability
import com.example.myproject.testing.FakeAppConfig
import com.example.myproject.testing.FakeProfileCache
import com.example.myproject.testing.FakeTaskRepository
import com.example.myproject.testing.awaitItemOfType
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
