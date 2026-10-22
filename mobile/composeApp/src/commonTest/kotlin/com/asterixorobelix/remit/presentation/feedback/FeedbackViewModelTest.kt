package com.asterixorobelix.remit.presentation.feedback

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.feedback.BuildFeedbackEmailUseCase
import com.asterixorobelix.remit.domain.feedback.ValidateFeedbackUseCase
import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {
    private lateinit var viewModel: FeedbackViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = FeedbackViewModel(ValidateFeedbackUseCase(), BuildFeedbackEmailUseCase(FakeAppConfig()))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankFieldsShowErrorsAndSendNothing() = runTest {
        viewModel.effects.test {
            viewModel.onEvent(FeedbackEvent.Submit("support@example.com"))
            expectNoEvents()
        }
        val state = assertIs<FeedbackUiState.Content>(viewModel.uiState.value)
        assertTrue(state.subjectError)
        assertTrue(state.messageError)
    }

    @Test
    fun editingClearsFieldError() {
        viewModel.onEvent(FeedbackEvent.Submit("support@example.com"))
        viewModel.onEvent(FeedbackEvent.SubjectChanged("Hello"))
        assertEquals(false, assertIs<FeedbackUiState.Content>(viewModel.uiState.value).subjectError)
    }

    @Test
    fun validFeedbackOpensMailDraftInsteadOfFakingSuccess() = runTest {
        viewModel.onEvent(FeedbackEvent.SubjectChanged("Idea"))
        viewModel.onEvent(FeedbackEvent.MessageChanged("Dark mode please"))
        viewModel.effects.test {
            viewModel.onEvent(FeedbackEvent.Submit("support@example.com"))
            val effect = assertIs<FeedbackEffect.ComposeEmail>(awaitItem())
            assertTrue(effect.mailtoUrl.startsWith("mailto:support@example.com?subject=Idea&body=Dark%20mode%20please"))
        }
    }
}
