package com.example.myproject.presentation.notifications

import com.example.myproject.data.notifications.EmptyNotificationsRepository
import com.example.myproject.domain.notifications.MarkNotificationReadUseCase
import com.example.myproject.domain.notifications.ObserveNotificationsUseCase
import com.example.myproject.domain.notifications.RefreshNotificationsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultInboxIsHonestlyEmpty() {
        val repository = EmptyNotificationsRepository()
        val viewModel = NotificationsViewModel(
            ObserveNotificationsUseCase(repository),
            RefreshNotificationsUseCase(repository),
            MarkNotificationReadUseCase(repository),
        )
        assertIs<NotificationsUiState.Empty>(viewModel.uiState.value)
    }
}
