package com.asterixorobelix.remit.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentStatusTest {
    @Test
    fun unknownCodesDoNotFailParsing() {
        assertEquals(PaymentStatus.COMPLETED, PaymentStatus.fromCode("COMPLETED"))
        assertEquals(PaymentStatus.FAILED, PaymentStatus.fromCode("FAILED"))
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode("REVERSED"))
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode("completed"))
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode(null))
        assertEquals(PaymentStatus.UNKNOWN, PaymentStatus.fromCode("UNKNOWN"))
    }
}
