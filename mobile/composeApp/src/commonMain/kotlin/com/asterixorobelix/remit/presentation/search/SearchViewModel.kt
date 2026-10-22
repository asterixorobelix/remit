package com.asterixorobelix.remit.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.search.ClearRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.GetRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.SaveRecentSearchUseCase
import com.asterixorobelix.remit.domain.search.SearchResult
import com.asterixorobelix.remit.domain.search.SearchUseCase
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.message_error_generic
import com.asterixorobelix.remit.util.runSuspendCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.StringResource

sealed interface SearchUiState {
    /** No query: shows recent searches. */
    data class Idle(val recentSearches: List<String> = emptyList()) : SearchUiState
    data object Loading : SearchUiState
    data class Content(val results: List<SearchResult>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: StringResource) : SearchUiState
}

sealed interface SearchEvent {
    data class QueryChanged(val query: String) : SearchEvent
    data object Submitted : SearchEvent
    data class RecentSearchClicked(val query: String) : SearchEvent
    data object ClearRecentSearches : SearchEvent
}

private const val DEBOUNCE_MS = 300L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val search: SearchUseCase,
    private val getRecentSearches: GetRecentSearchesUseCase,
    private val saveRecentSearch: SaveRecentSearchUseCase,
    private val clearRecentSearches: ClearRecentSearchesUseCase,
) : ViewModel() {

    private val mutableQuery = MutableStateFlow("")
    val query: StateFlow<String> = mutableQuery.asStateFlow()

    private val mutableState = MutableStateFlow<SearchUiState>(SearchUiState.Idle(getRecentSearches()))
    val uiState: StateFlow<SearchUiState> = mutableState.asStateFlow()

    init {
        mutableQuery
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce(DEBOUNCE_MS)
            .flatMapLatest { query -> flow { emit(stateFor(query)) } }
            .onEach { mutableState.value = it }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChanged -> {
                mutableQuery.value = event.query
                if (event.query.isNotBlank()) mutableState.value = SearchUiState.Loading
            }

            SearchEvent.Submitted -> saveRecentSearch(mutableQuery.value)

            is SearchEvent.RecentSearchClicked -> onEvent(SearchEvent.QueryChanged(event.query))

            SearchEvent.ClearRecentSearches -> {
                clearRecentSearches()
                if (mutableState.value is SearchUiState.Idle) mutableState.value = SearchUiState.Idle()
            }
        }
    }

    private suspend fun stateFor(query: String): SearchUiState {
        if (query.isEmpty()) return SearchUiState.Idle(getRecentSearches())
        return runSuspendCatching { search(query) }.fold(
            onSuccess = { results -> if (results.isEmpty()) SearchUiState.Empty else SearchUiState.Content(results) },
            onFailure = { SearchUiState.Error(Res.string.message_error_generic) },
        )
    }
}
