package com.asterixorobelix.remit.shared.domain.model

/**
 * Currencies the app can send. [minorDigits] is the ISO 4217 exponent: how many decimal places
 * the currency allows (2 for cents). Amounts are always stored as whole minor units.
 */
enum class Currency(val code: String, val minorDigits: Int, val symbol: String) {
    USD(code = "USD", minorDigits = 2, symbol = "$"),
    EUR(code = "EUR", minorDigits = 2, symbol = "€"),
    ;

    companion object {
        /** Exact, case-sensitive ISO 4217 lookup: "usd" is not a supported code. */
        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code == code }
    }
}
