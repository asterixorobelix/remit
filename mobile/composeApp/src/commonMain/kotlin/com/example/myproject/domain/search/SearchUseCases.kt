package com.example.myproject.domain.search

class SearchUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(query: String): List<SearchResult> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) emptyList() else repository.search(trimmed)
    }
}

class GetRecentSearchesUseCase(private val repository: RecentSearchesRepository) {
    operator fun invoke(): List<String> = repository.recentSearches()
}

class SaveRecentSearchUseCase(private val repository: RecentSearchesRepository) {
    operator fun invoke(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) repository.add(trimmed)
    }
}

class ClearRecentSearchesUseCase(private val repository: RecentSearchesRepository) {
    operator fun invoke() = repository.clear()
}
