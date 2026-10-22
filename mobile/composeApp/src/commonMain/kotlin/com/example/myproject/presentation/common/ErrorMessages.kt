package com.example.myproject.presentation.common

import com.example.myproject.domain.auth.CredentialsError
import com.example.myproject.domain.common.DataError
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.auth_error_all_fields_required
import com.example.myproject.generated.resources.auth_error_email_taken
import com.example.myproject.generated.resources.auth_error_invalid_credentials
import com.example.myproject.generated.resources.auth_error_passwords_do_not_match
import com.example.myproject.generated.resources.message_error_generic
import com.example.myproject.generated.resources.message_error_invalid_input
import com.example.myproject.generated.resources.message_error_network
import com.example.myproject.generated.resources.message_error_rate_limited
import com.example.myproject.generated.resources.message_error_server
import com.example.myproject.generated.resources.message_error_session_expired
import com.example.myproject.generated.resources.validation_email_invalid
import com.example.myproject.generated.resources.validation_password_short
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
