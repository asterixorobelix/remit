package com.asterixorobelix.remit.presentation.payments

import com.asterixorobelix.remit.shared.domain.model.Money
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val GROUP_SIZE = 3

/** "$1,234.50": the currency symbol, grouped whole units, and exactly the currency's decimals. */
fun Money.formatted(): String {
    val plain = toDecimalString()
    val whole = plain.substringBefore('.')
    val fraction = plain.substringAfter('.', missingDelimiterValue = "")
    val decimals = if (fraction.isEmpty()) "" else ".$fraction"
    return currency.symbol + groupThousands(whole) + decimals
}

/** "10000" becomes "10,000". */
fun groupThousands(digits: String): String =
    digits.reversed().chunked(GROUP_SIZE).joinToString(",").reversed()

private val timestampFormat = LocalDateTime.Format {
    day(padding = Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
    chars(", ")
    hour()
    char(':')
    minute()
}

/** "22 Oct 2026, 10:30" in [timeZone] (the device's by default). */
@OptIn(ExperimentalTime::class)
fun Instant.formattedLocal(timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    toLocalDateTime(timeZone).format(timestampFormat)
