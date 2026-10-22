package com.asterixorobelix.remit.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals

class SessionUseCasesTest {
    private val initials = ComputeInitialsUseCase()

    @Test
    fun usesFirstTwoNameInitials() {
        assertEquals("JR", initials(UserProfile("john ronald doe", "j@example.com")))
    }

    @Test
    fun singleNameGivesOneInitial() {
        assertEquals("J", initials(UserProfile("John", "j@example.com")))
    }

    @Test
    fun fallsBackToEmail() {
        assertEquals("M", initials(UserProfile("", "mary@example.com")))
    }
}
