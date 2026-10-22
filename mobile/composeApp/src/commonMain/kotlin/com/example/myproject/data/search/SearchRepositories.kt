package com.example.myproject.data.search

import com.example.myproject.data.preferences.AppPreferences
import com.example.myproject.domain.search.RecentSearchesRepository
import com.example.myproject.domain.search.SearchRepository
import com.example.myproject.domain.search.SearchResult
import com.example.myproject.domain.task.TaskRepository

private const val MAX_RESULTS = 50

/** Searches task titles in the local database (`offline` capability). */
internal class TaskSearchRepository(private val tasks: TaskRepository) : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> =
        tasks.search(query, MAX_RESULTS).map { SearchResult(it.id, it.title, it.completed) }
}

/** Used when no searchable content source is enabled. */
internal class EmptySearchRepository : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> = emptyList()
}

internal class PreferencesRecentSearchesRepository(
    private val preferences: AppPreferences,
) : RecentSearchesRepository {
    override fun recentSearches(): List<String> = preferences.getRecentSearches()
    override fun add(query: String) = preferences.addRecentSearch(query)
    override fun clear() = preferences.clearRecentSearches()
}
