package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.DecimalAmount

/**
 * What the amount field accepts while the user types: ASCII digits and at most one `.` or `,`.
 * Anything else is dropped, so letters, spaces, signs, and currency symbols never appear.
 *
 * Extra decimal places are deliberately let through for validation to flag instead of being cut
 * off: truncating would turn "1,000" (one thousand, typed with a grouping separator) into "1,00".
 * An edit that would leave two separators, such as pasting "$1,234.56", is ignored as a whole
 * rather than guessed at.
 */
object AmountInput {
    /** Longest accepted text: the most whole and fractional digits a valid amount can have, plus a separator. */
    const val MAX_LENGTH = DecimalAmount.MAX_WHOLE_DIGITS + 1 + DecimalAmount.MAX_FRACTION_DIGITS

    fun filter(previous: String, proposed: String): String {
        val kept = proposed.filter { it in '0'..'9' || it == '.' || it == ',' }
        val separators = kept.count { it == '.' || it == ',' }
        return if (separators > 1 || kept.length > MAX_LENGTH) previous else kept
    }
}

/** Applies [AmountInput] to each edit of the amount field. */
class FilterAmountInputUseCase {
    operator fun invoke(previous: String, proposed: String): String = AmountInput.filter(previous, proposed)
}
