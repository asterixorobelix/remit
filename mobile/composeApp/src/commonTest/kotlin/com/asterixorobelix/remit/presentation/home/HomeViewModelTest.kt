package com.asterixorobelix.remit.presentation.home

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.home.DayPeriod
import com.asterixorobelix.remit.domain.home.GetDayPeriodUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheGreetingForTheCurrentDayPeriod() = runTest {
        val getDayPeriod = GetDayPeriodUseCase()
        val viewModel = HomeViewModel(getDayPeriod)
        viewModel.uiState.test {
            assertEquals(getDayPeriod(), awaitItemOfType<HomeUiState.Content>().dayPeriod)
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
