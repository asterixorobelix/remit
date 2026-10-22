package com.asterixorobelix.remit.presentation.common

import com.asterixorobelix.remit.domain.auth.CredentialsError
import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.auth_error_all_fields_required
import com.asterixorobelix.remit.generated.resources.auth_error_email_taken
import com.asterixorobelix.remit.generated.resources.auth_error_invalid_credentials
import com.asterixorobelix.remit.generated.resources.auth_error_passwords_do_not_match
import com.asterixorobelix.remit.generated.resources.message_error_generic
import com.asterixorobelix.remit.generated.resources.message_error_invalid_input
import com.asterixorobelix.remit.generated.resources.message_error_network
import com.asterixorobelix.remit.generated.resources.message_error_rate_limited
import com.asterixorobelix.remit.generated.resources.message_error_server
import com.asterixorobelix.remit.generated.resources.message_error_session_expired
import com.asterixorobelix.remit.generated.resources.validation_email_invalid
import com.asterixorobelix.remit.generated.resources.validation_password_short
import org.jetbrains.compose.resources.StringResource

/** User-facing message for a typed data failure. */
fun DataError.toMessage(): StringResource = when (this) {
    DataError.NETWORK -> Res.string.message_error_network
    DataError.INVALID_CREDENTIALS -> Res.string.auth_error_invalid_credentials
    DataError.EMAIL_TAKEN -> Res.string.auth_error_email_taken
    DataError.INVALID_INPUT -> Res.string.message_error_invalid_input
    DataError.UNAUTHORIZED -> Res.string.message_error_session_expired
    DataError.RATE_LIMITED -> Res.string.message_error_rate_limited
    DataError.SERVER -> Res.string.message_error_server
    DataError.UNKNOWN -> Res.string.message_error_generic
}

fun CredentialsError.toMessage(): StringResource = when (this) {
    CredentialsError.MISSING_FIELDS -> Res.string.auth_error_all_fields_required
    CredentialsError.INVALID_EMAIL -> Res.string.validation_email_invalid
    CredentialsError.PASSWORD_TOO_SHORT -> Res.string.validation_password_short
    CredentialsError.PASSWORDS_DO_NOT_MATCH -> Res.string.auth_error_passwords_do_not_match
}
