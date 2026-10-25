package com.asterixorobelix.remit.data.developer

import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.domain.developer.DeveloperSettingsRepository

internal class PreferencesDeveloperSettingsRepository(
    private val preferences: AppPreferences,
) : DeveloperSettingsRepository {
    override suspend fun apiBaseUrlOverride(): String? = preferences.getApiBaseUrlOverride()
    override suspend fun setApiBaseUrlOverride(url: String?) = preferences.setApiBaseUrlOverride(url)
    override suspend fun resetLocalState() = preferences.resetForDevelopment()
}
