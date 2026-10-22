package com.asterixorobelix.remit.presentation.search

import com.asterixorobelix.remit.data.preferences.FakeAppPreferences
import com.asterixorobelix.remit.data.search.EmptySearchRepository
import com.asterixorobelix.remit.data.search.PreferencesRecentSearchesRepository
import com.asterixorobelix.remit.data.search.TaskSearchRepository
import com.asterixorobelix.remit.domain.search.ClearRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.GetRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.SaveRecentSearchUseCase
import com.asterixorobelix.remit.domain.search.SearchRepository
import com.asterixorobelix.remit.domain.search.SearchUseCase
import com.asterixorobelix.remit.domain.task.Task
import com.asterixorobelix.remit.testing.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences = FakeAppPreferences()
    private val tasks = FakeTaskRepository()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: SearchRepository) = SearchViewModel(
        search = SearchUseCase(repository),
        getRecentSearches = GetRecentSearchesUseCase(PreferencesRecentSearchesRepository(preferences)),
        saveRecentSearch = SaveRecentSearchUseCase(PreferencesRecentSearchesRepository(preferences)),
        clearRecentSearches = ClearRecentSearchesUseCase(PreferencesRecentSearchesRepository(preferences)),
    )

    private fun TestScope.search(viewModel: SearchViewModel, query: String) {
        viewModel.onEvent(SearchEvent.QueryChanged(query))
        advanceUntilIdle()
    }

    @Test
    fun searchesLocalTasks() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            Task("1", "Buy milk", completed = false, updatedAt = 1, createdAt = 1),
            Task("2", "Ship release", completed = true, updatedAt = 1, createdAt = 1),
        )
        val viewModel = viewModel(TaskSearchRepository(tasks))
        search(viewModel, "ship")
        val content = assertIs<SearchUiState.Content>(viewModel.uiState.value)
        assertEquals(listOf("2"), content.results.map { it.id })
        assertEquals(true, content.results.single().completed)
    }

    @Test
    fun withoutSearchableContentShowsEmptyNotFakeResults() = runTest(dispatcher) {
        val viewModel = viewModel(EmptySearchRepository())
        search(viewModel, "anything")
        assertIs<SearchUiState.Empty>(viewModel.uiState.value)
    }

    @Test
    fun submittedQueriesBecomeRecentSearches() = runTest(dispatcher) {
        val viewModel = viewModel(EmptySearchRepository())
        search(viewModel, "kotlin")
        viewModel.onEvent(SearchEvent.Submitted)
        search(viewModel, "")
        assertEquals(listOf("kotlin"), assertIs<SearchUiState.Idle>(viewModel.uiState.value).recentSearches)

        viewModel.onEvent(SearchEvent.ClearRecentSearches)
        assertEquals(emptyList(), assertIs<SearchUiState.Idle>(viewModel.uiState.value).recentSearches)
    }
}
