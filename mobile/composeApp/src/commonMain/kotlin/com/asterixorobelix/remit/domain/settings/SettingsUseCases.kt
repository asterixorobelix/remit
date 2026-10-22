package com.asterixorobelix.remit.domain.settings

import com.asterixorobelix.remit.domain.session.SessionRepository

class ObserveThemeModeUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke() = repository.themeMode
}

class SetThemeModeUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class CompleteOnboardingUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke() = repository.completeOnboarding()
}

/** Records a launch; returns true exactly once, on the launch that should ask for a store review. */
class RecordAppOpenUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke(): Boolean {
        val opens = repository.recordAppOpen()
        return opens == REVIEW_PROMPT_OPEN_COUNT && !repository.hasRequestedReview()
    }

    private companion object {
        const val REVIEW_PROMPT_OPEN_COUNT = 5
    }
}

class MarkReviewRequestedUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke() = repository.markReviewRequested()
}

enum class StartDestination { ONBOARDING, LOGIN, HOME }

/** Onboarding first, then sign-in when the app requires accounts and no session exists. */
class ResolveStartDestinationUseCase(
    private val settings: AppSettingsRepository,
    private val session: SessionRepository,
) {
    suspend operator fun invoke(authEnabled: Boolean): StartDestination = when {
        !settings.hasCompletedOnboarding() -> StartDestination.ONBOARDING
        authEnabled && !session.hasSession() -> StartDestination.LOGIN
        else -> StartDestination.HOME
    }
}
