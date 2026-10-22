package com.asterixorobelix.remit.domain.feedback

import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedbackUseCasesTest {
    @Test
    fun validationFlagsBlankFields() {
        val result = ValidateFeedbackUseCase()(" ", "")
        assertTrue(result.subjectMissing)
        assertTrue(result.messageMissing)
        assertFalse(result.isValid)
        assertTrue(ValidateFeedbackUseCase()("Hi", "There").isValid)
    }

    @Test
    fun buildsPercentEncodedMailtoWithDiagnostics() {
        val url = BuildFeedbackEmailUseCase(FakeAppConfig(versionName = "1.2.0", versionCode = 10200))(
            recipient = "support@example.com",
            subject = "Bug & crash",
            message = "Café broke",
        )
        assertEquals(
            "mailto:support@example.com?subject=Bug%20%26%20crash" +
                "&body=Caf%C3%A9%20broke%0A%0A--%0Acom.asterixorobelix.remit%201.2.0%20%2810200%29",
            url,
        )
    }
}
