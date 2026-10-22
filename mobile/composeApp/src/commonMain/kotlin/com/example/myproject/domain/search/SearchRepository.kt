package com.example.myproject.domain.search

data class SearchResult(
    val id: String,
    val title: String,
    val completed: Boolean,
)

/**
 * Searches the app's content. The template indexes local Tasks when the `offline` capability is
 * enabled; otherwise nothing is searchable and the screen shows its empty state. Replace or extend
 * the implementation with your app's own content sources.
 */
interface SearchRepository {
    suspend fun search(query: String): List<SearchResult>
}

interface RecentSearchesRepository {
    fun recentSearches(): List<String>
    fun add(query: String)
    fun clear()
}
