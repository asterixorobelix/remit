package com.asterixorobelix.remit.presentation.common

import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.message_error_generic
import com.asterixorobelix.remit.generated.resources.message_error_invalid_input
import com.asterixorobelix.remit.generated.resources.message_error_network
import com.asterixorobelix.remit.generated.resources.message_error_rate_limited
import com.asterixorobelix.remit.generated.resources.message_error_server
import com.asterixorobelix.remit.generated.resources.message_error_unauthorized
import org.jetbrains.compose.resources.StringResource

/** User-facing message for a typed data failure. */
fun DataError.toMessage(): StringResource = when (this) {
    DataError.NETWORK -> Res.string.message_error_network
    DataError.INVALID_INPUT -> Res.string.message_error_invalid_input
    DataError.UNAUTHORIZED -> Res.string.message_error_unauthorized
    DataError.RATE_LIMITED -> Res.string.message_error_rate_limited
    DataError.SERVER -> Res.string.message_error_server
    DataError.UNKNOWN -> Res.string.message_error_generic
}
