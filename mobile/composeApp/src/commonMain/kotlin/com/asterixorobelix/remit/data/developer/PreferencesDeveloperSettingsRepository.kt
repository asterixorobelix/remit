package com.asterixorobelix.remit.data.developer

import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.domain.developer.DeveloperSettingsRepository

internal class PreferencesDeveloperSettingsRepository(
    private val preferences: AppPreferences,
) : DeveloperSettingsRepository {
    override fun apiBaseUrlOverride(): String? = preferences.getApiBaseUrlOverride()
    override fun setApiBaseUrlOverride(url: String?) = preferences.setApiBaseUrlOverride(url)
    override fun resetLocalState() = preferences.resetForDevelopment()
}
