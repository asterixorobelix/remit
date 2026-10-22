package com.asterixorobelix.remit.domain.feedback

import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.util.percentEncode

data class FeedbackValidation(val subjectMissing: Boolean, val messageMissing: Boolean) {
    val isValid: Boolean get() = !subjectMissing && !messageMissing
}

class ValidateFeedbackUseCase {
    operator fun invoke(subject: String, message: String) =
        FeedbackValidation(subjectMissing = subject.isBlank(), messageMissing = message.isBlank())
}

/**
 * Builds a `mailto:` URL so feedback is sent from the user's own mail app. This needs no backend;
 * replace it with an API call if your app collects feedback server-side.
 */
class BuildFeedbackEmailUseCase(private val appConfig: AppConfig) {
    operator fun invoke(recipient: String, subject: String, message: String): String {
        val body = buildString {
            append(message.trim())
            append("\n\n--\n")
            append("${appConfig.applicationId} ${appConfig.versionName} (${appConfig.versionCode})")
        }
        return "mailto:${recipient.trim()}?subject=${subject.trim().percentEncode()}&body=${body.percentEncode()}"
    }
}
