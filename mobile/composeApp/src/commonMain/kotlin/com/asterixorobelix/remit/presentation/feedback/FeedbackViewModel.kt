package com.asterixorobelix.remit.presentation.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.feedback.BuildFeedbackEmailUseCase
import com.asterixorobelix.remit.domain.feedback.ValidateFeedbackUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface FeedbackUiState {
    data class Content(
        val subject: String = "",
        val message: String = "",
        val subjectError: Boolean = false,
        val messageError: Boolean = false,
    ) : FeedbackUiState
}

sealed interface FeedbackEvent {
    data class SubjectChanged(val value: String) : FeedbackEvent
    data class MessageChanged(val value: String) : FeedbackEvent

    /** [recipient] is the support address from string resources. */
    data class Submit(val recipient: String) : FeedbackEvent
}

sealed interface FeedbackEffect {
    /** Open the mail app with a prefilled draft; the screen reports if no mail app exists. */
    data class ComposeEmail(val mailtoUrl: String) : FeedbackEffect
}

class FeedbackViewModel(
    private val validateFeedback: ValidateFeedbackUseCase,
    private val buildFeedbackEmail: BuildFeedbackEmailUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(FeedbackUiState.Content())
    val uiState: StateFlow<FeedbackUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<FeedbackEffect>(Channel.BUFFERED)
    val effects: Flow<FeedbackEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: FeedbackEvent) {
        when (event) {
            is FeedbackEvent.SubjectChanged -> mutableState.update {
                it.copy(subject = event.value, subjectError = false)
            }

            is FeedbackEvent.MessageChanged -> mutableState.update {
                it.copy(message = event.value, messageError = false)
            }

            is FeedbackEvent.Submit -> submit(event.recipient)
        }
    }

    private fun submit(recipient: String) {
        val current = mutableState.value
        val validation = validateFeedback(current.subject, current.message)
        if (!validation.isValid) {
            mutableState.value = current.copy(
                subjectError = validation.subjectMissing,
                messageError = validation.messageMissing,
            )
            return
        }
        val url = buildFeedbackEmail(recipient, current.subject, current.message)
        viewModelScope.launch { effectChannel.send(FeedbackEffect.ComposeEmail(url)) }
    }
}
