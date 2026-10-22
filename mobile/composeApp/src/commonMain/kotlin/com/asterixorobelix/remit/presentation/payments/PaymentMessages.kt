package com.asterixorobelix.remit.presentation.payments

import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.error_amount_above_limit
import com.asterixorobelix.remit.generated.resources.error_amount_blank
import com.asterixorobelix.remit.generated.resources.error_amount_not_a_number
import com.asterixorobelix.remit.generated.resources.error_amount_not_positive
import com.asterixorobelix.remit.generated.resources.error_amount_too_many_decimals
import com.asterixorobelix.remit.generated.resources.error_currency_missing
import com.asterixorobelix.remit.generated.resources.error_currency_unsupported
import com.asterixorobelix.remit.generated.resources.error_email_blank
import com.asterixorobelix.remit.generated.resources.error_email_invalid
import com.asterixorobelix.remit.generated.resources.error_email_too_long
import com.asterixorobelix.remit.generated.resources.history_status_completed
import com.asterixorobelix.remit.generated.resources.history_status_failed
import com.asterixorobelix.remit.generated.resources.history_status_pending
import com.asterixorobelix.remit.generated.resources.history_status_unknown
import com.asterixorobelix.remit.generated.resources.payment_failed_conflict
import com.asterixorobelix.remit.generated.resources.payment_failed_network
import com.asterixorobelix.remit.generated.resources.payment_failed_rate_limited
import com.asterixorobelix.remit.generated.resources.payment_failed_server
import com.asterixorobelix.remit.generated.resources.payment_failed_timeout
import com.asterixorobelix.remit.generated.resources.payment_failed_unauthorized
import com.asterixorobelix.remit.generated.resources.payment_failed_unexpected
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import org.jetbrains.compose.resources.StringResource

/** The field message for each validation code (the API returns the same codes). */
val ValidationError.message: StringResource
    get() = when (this) {
        ValidationError.EMAIL_BLANK -> Res.string.error_email_blank
        ValidationError.EMAIL_TOO_LONG -> Res.string.error_email_too_long
        ValidationError.EMAIL_INVALID -> Res.string.error_email_invalid
        ValidationError.AMOUNT_BLANK -> Res.string.error_amount_blank
        ValidationError.AMOUNT_NOT_A_NUMBER -> Res.string.error_amount_not_a_number
        ValidationError.AMOUNT_NOT_POSITIVE -> Res.string.error_amount_not_positive
        ValidationError.AMOUNT_TOO_MANY_DECIMALS -> Res.string.error_amount_too_many_decimals
        ValidationError.AMOUNT_ABOVE_LIMIT -> Res.string.error_amount_above_limit
        ValidationError.CURRENCY_MISSING -> Res.string.error_currency_missing
        ValidationError.CURRENCY_UNSUPPORTED -> Res.string.error_currency_unsupported
    }

val PaymentFailure.message: StringResource
    get() = when (this) {
        PaymentFailure.NETWORK -> Res.string.payment_failed_network
        PaymentFailure.TIMEOUT -> Res.string.payment_failed_timeout
        PaymentFailure.SERVER -> Res.string.payment_failed_server
        PaymentFailure.RATE_LIMITED -> Res.string.payment_failed_rate_limited
        PaymentFailure.UNAUTHORIZED -> Res.string.payment_failed_unauthorized
        PaymentFailure.IDEMPOTENCY_CONFLICT -> Res.string.payment_failed_conflict
        PaymentFailure.UNEXPECTED_RESPONSE -> Res.string.payment_failed_unexpected
    }

val PaymentStatus.label: StringResource
    get() = when (this) {
        PaymentStatus.COMPLETED -> Res.string.history_status_completed
        PaymentStatus.PENDING -> Res.string.history_status_pending
        PaymentStatus.FAILED -> Res.string.history_status_failed
        PaymentStatus.UNKNOWN -> Res.string.history_status_unknown
    }
