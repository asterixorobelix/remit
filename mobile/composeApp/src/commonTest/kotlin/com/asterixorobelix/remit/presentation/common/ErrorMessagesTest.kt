package com.asterixorobelix.remit.presentation.common

import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.message_error_generic
import com.asterixorobelix.remit.generated.resources.message_error_network
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {
    @Test
    fun everyErrorHasItsOwnMessage() {
        val messages = DataError.entries.map { it.toMessage() }
        assertEquals(DataError.entries.size, messages.toSet().size)
        assertEquals(Res.string.message_error_network, DataError.NETWORK.toMessage())
        assertEquals(Res.string.message_error_generic, DataError.UNKNOWN.toMessage())
    }
}
