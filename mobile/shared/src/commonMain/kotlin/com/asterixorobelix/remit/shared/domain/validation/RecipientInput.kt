package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.EmailAddress

/**
 * What the recipient field accepts while the user types: at most [MAX_LENGTH] characters, the
 * longest address validation and the server accept. An edit that would go past it, such as a long
 * paste, is ignored as a whole rather than cut off into a different address. Shortening is always
 * allowed, so an over-long value can still be corrected.
 */
object RecipientInput {
    const val MAX_LENGTH = EmailAddress.MAX_LENGTH

    fun filter(previous: String, proposed: String): String =
        if (proposed.length > MAX_LENGTH && proposed.length > previous.length) previous else proposed
}

/** Applies [RecipientInput] to each edit of the recipient field. */
class FilterRecipientInputUseCase {
    operator fun invoke(previous: String, proposed: String): String = RecipientInput.filter(previous, proposed)
}
