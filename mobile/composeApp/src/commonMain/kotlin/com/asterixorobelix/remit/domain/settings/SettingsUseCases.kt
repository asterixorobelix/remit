package com.asterixorobelix.remit.domain.settings

class ObserveThemeModeUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke() = repository.themeMode
}

class SetThemeModeUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class CompleteOnboardingUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke() = repository.completeOnboarding()
}

/** Records a launch; returns true exactly once, on the launch that should ask for a store review. */
class RecordAppOpenUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(): Boolean {
        val opens = repository.recordAppOpen()
        return opens == REVIEW_PROMPT_OPEN_COUNT && !repository.hasRequestedReview()
    }

    private companion object {
        const val REVIEW_PROMPT_OPEN_COUNT = 5
    }
}

class MarkReviewRequestedUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke() = repository.markReviewRequested()
}

enum class StartDestination { ONBOARDING, HOME }

/** Onboarding on first launch, Home afterwards. */
class ResolveStartDestinationUseCase(private val settings: AppSettingsRepository) {
    suspend operator fun invoke(): StartDestination =
        if (settings.hasCompletedOnboarding()) StartDestination.HOME else StartDestination.ONBOARDING
}
