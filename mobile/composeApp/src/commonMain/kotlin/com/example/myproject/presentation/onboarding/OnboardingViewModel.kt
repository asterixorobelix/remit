package com.example.myproject.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.settings.CompleteOnboardingUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface OnboardingEffect {
    data object Completed : OnboardingEffect
}

class OnboardingViewModel(private val completeOnboarding: CompleteOnboardingUseCase) : ViewModel() {
    private val effectChannel = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = effectChannel.receiveAsFlow()

    fun onFinish() {
        completeOnboarding()
        viewModelScope.launch { effectChannel.send(OnboardingEffect.Completed) }
    }
}
