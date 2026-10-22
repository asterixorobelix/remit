package com.asterixorobelix.remit.shared.domain.model

/**
 * An exact, non-negative decimal read from text without ever passing through floating point:
 * the value is [unscaled] / 10^[scale], so "10.50" is `DecimalAmount(unscaled = 1050, scale = 2)`.
 *
 * Parsing accepts what people type into an amount field: digits with at most one `.` or `,` as the
 * decimal separator ("10.50", "10,50", ".5", "5."). Signs, exponents, grouping separators, and
 * non-ASCII digits are rejected, so "1,000.00" and "1e3" never become a different number.
 */
data class DecimalAmount(val unscaled: Long, val scale: Int) {
    val isZero: Boolean get() = unscaled == 0L

    /**
     * The amount in [currency]'s minor units, or null when it has more decimal places than the
     * currency allows. Trailing zeros count: "1,000" in USD is three decimals and is rejected
     * rather than read as 1.00, which protects people who type a thousands separator.
     */
    fun toMoney(currency: Currency): Money? {
        if (scale > currency.minorDigits) return null
        val minorUnits = (scale until currency.minorDigits).fold(unscaled) { value, _ -> value * DECIMAL_BASE }
        return Money(minorUnits, currency)
    }

    companion object {
        /** Whole-number digits; anything longer is far above any payment limit. */
        const val MAX_WHOLE_DIGITS = 13

        /** More decimals than any currency has (ISO 4217 goes to 4); keeps the value inside [Long]. */
        const val MAX_FRACTION_DIGITS = 5
        private const val DECIMAL_BASE = 10L
        private val DECIMAL = Regex("""^([0-9]*)(?:[.,]([0-9]*))?$""")

        fun parse(text: String): DecimalParseResult {
            val trimmed = text.trim()
            val match = DECIMAL.matchEntire(trimmed)
            val whole = match?.groupValues?.get(1).orEmpty()
            val fraction = match?.groupValues?.get(2).orEmpty()
            return when {
                trimmed.isEmpty() -> DecimalParseResult.Failure.BLANK

                match == null || (whole.isEmpty() && fraction.isEmpty()) -> DecimalParseResult.Failure.NOT_A_NUMBER

                whole.trimStart('0').length > MAX_WHOLE_DIGITS -> DecimalParseResult.Failure.OUT_OF_RANGE

                fraction.length > MAX_FRACTION_DIGITS -> DecimalParseResult.Failure.TOO_PRECISE

                else -> DecimalParseResult.Parsed(
                    DecimalAmount((whole + fraction).trimStart('0').ifEmpty { "0" }.toLong(), fraction.length),
                )
            }
        }
    }
}

sealed interface DecimalParseResult {
    data class Parsed(val amount: DecimalAmount) : DecimalParseResult

    enum class Failure : DecimalParseResult {
        BLANK,
        NOT_A_NUMBER,

        /** Too many whole digits to be a payment. */
        OUT_OF_RANGE,

        /** Too many decimals for any currency, such as the float artefact 0.30000000000000004. */
        TOO_PRECISE,
    }
}
