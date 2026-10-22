package com.example.myproject.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myproject.domain.search.SearchResult
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_navigate_back
import com.example.myproject.generated.resources.detail_status_completed
import com.example.myproject.generated.resources.detail_status_open
import com.example.myproject.generated.resources.screen_search_title
import com.example.myproject.generated.resources.search_clear
import com.example.myproject.generated.resources.search_clear_recent
import com.example.myproject.generated.resources.search_empty
import com.example.myproject.generated.resources.search_empty_description
import com.example.myproject.generated.resources.search_hint
import com.example.myproject.generated.resources.search_recent
import com.example.myproject.ui.components.EmptyState
import com.example.myproject.ui.components.FullScreenError
import com.example.myproject.ui.components.LoadingIndicator
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onResultClick: (SearchResult) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SearchContent(
            query = query,
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onResultClick = onResultClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun SearchContent(
    query: String,
    uiState: SearchUiState,
    onEvent: (SearchEvent) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screenHorizontalPadding),
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        SearchBar(
            query = query,
            onQueryChange = { onEvent(SearchEvent.QueryChanged(it)) },
            onSearchSubmit = { onEvent(SearchEvent.Submitted) },
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        when (uiState) {
            is SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }

            is SearchUiState.Idle -> RecentSearchesSection(
                recentSearches = uiState.recentSearches,
                onRecentSearchClick = { onEvent(SearchEvent.RecentSearchClicked(it)) },
                onClearRecentSearches = { onEvent(SearchEvent.ClearRecentSearches) },
            )

            is SearchUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(Res.string.search_empty),
                    description = stringResource(Res.string.search_empty_description),
                )
            }

            is SearchUiState.Error -> FullScreenError(message = stringResource(uiState.message))

            is SearchUiState.Content -> SearchResultsList(
                results = uiState.results,
                onResultClick = { result ->
                    onEvent(SearchEvent.Submitted)
                    onResultClick(result)
                },
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(text = stringResource(Res.string.search_hint))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.search_clear),
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
        shape = MaterialTheme.shapes.medium,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onRecentSearchClick: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
) {
    if (recentSearches.isEmpty()) return

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.search_recent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onClearRecentSearches) {
                Text(text = stringResource(Res.string.search_clear_recent))
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            recentSearches.forEach { search ->
                AssistChip(
                    onClick = { onRecentSearchClick(search) },
                    label = { Text(text = search) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: List<SearchResult>,
    onResultClick: (SearchResult) -> Unit,
) {
    LazyColumn {
        items(results, key = { it.id }) { item ->
            ListItem(
                headlineContent = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(
                            if (item.completed) Res.string.detail_status_completed else Res.string.detail_status_open,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(role = Role.Button) { onResultClick(item) },
            )
        }
    }
}
