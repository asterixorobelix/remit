package com.example.myproject.domain.auth

import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.common.toDataError
import com.example.myproject.testing.FakeAuthRepository
import com.example.myproject.testing.RecordingCleaner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthUseCasesTest {
    private val repository = FakeAuthRepository()
    private val cleaner = RecordingCleaner()

    @Test
    fun loginNormalizesEmail() = runTest {
        LoginUseCase(repository)(" User@Example.COM ", "password1")
        assertEquals("user@example.com", repository.lastEmail)
    }

    @Test
    fun registerTrimsNameAndNormalizesEmail() = runTest {
        RegisterUseCase(repository)("  Ada Lovelace ", " ADA@example.com", "password1")
        assertEquals("Ada Lovelace", repository.lastDisplayName)
        assertEquals("ada@example.com", repository.lastEmail)
    }

    @Test
    fun logoutRevokesSessionAndClearsEveryFeaturesUserData() = runTest {
        LogoutUseCase(repository, listOf(cleaner, cleaner))()
        assertEquals(1, repository.logoutCalls)
        assertEquals(2, cleaner.calls)
    }

    @Test
    fun deleteAccountClearsLocalDataOnlyAfterServerConfirms() = runTest {
        val useCase = DeleteAccountUseCase(repository, listOf(cleaner))

        repository.failure = DataError.NETWORK
        val failed = useCase()
        assertEquals(DataError.NETWORK, failed.exceptionOrNull()?.toDataError())
        assertEquals(0, cleaner.calls)

        repository.failure = null
        assertTrue(useCase().isSuccess)
        assertEquals(1, cleaner.calls)
    }

    @Test
    fun updateDisplayNameRejectsBlankAndTrims() = runTest {
        val useCase = UpdateDisplayNameUseCase(repository)
        assertEquals(DataError.INVALID_INPUT, useCase("   ").exceptionOrNull()?.toDataError())
        assertEquals("Grace", useCase("  Grace ").getOrThrow().displayName)
        assertEquals("Grace", repository.lastDisplayName)
    }
}
