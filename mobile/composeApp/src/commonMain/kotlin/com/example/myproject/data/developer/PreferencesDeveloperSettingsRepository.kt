package com.example.myproject.data.developer

import com.example.myproject.data.preferences.AppPreferences
import com.example.myproject.domain.developer.DeveloperSettingsRepository

internal class PreferencesDeveloperSettingsRepository(
    private val preferences: AppPreferences,
) : DeveloperSettingsRepository {
    override fun apiBaseUrlOverride(): String? = preferences.getApiBaseUrlOverride()
    override fun setApiBaseUrlOverride(url: String?) = preferences.setApiBaseUrlOverride(url)
    override fun resetLocalState() = preferences.resetForDevelopment()
}
