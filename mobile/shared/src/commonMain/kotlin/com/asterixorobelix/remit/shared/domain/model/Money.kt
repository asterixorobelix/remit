package com.asterixorobelix.remit.shared.domain.model

/**
 * An exact amount of money: a whole number of [currency]'s minor units (cents for USD and EUR).
 * Never a `Double`, so 0.10 + 0.20 is exactly 0.30 and nothing is lost on the way to the server.
 */
data class Money(val minorUnits: Long, val currency: Currency) : Comparable<Money> {
    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare ${currency.code} with ${other.currency.code}" }
        return minorUnits.compareTo(other.minorUnits)
    }

    /** Plain decimal text with exactly the currency's decimal places and no symbol: "10.50". */
    fun toDecimalString(): String {
        val digits = currency.minorDigits
        val sign = if (minorUnits < 0) "-" else ""
        // toString() and removePrefix, not absoluteValue, which overflows for Long.MIN_VALUE.
        val padded = minorUnits.toString().removePrefix("-").padStart(digits + 1, '0')
        val fraction = if (digits == 0) "" else "." + padded.takeLast(digits)
        return sign + padded.dropLast(digits) + fraction
    }
}
