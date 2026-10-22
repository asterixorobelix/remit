package com.asterixorobelix.remit.presentation.payments

/**
 * Stable test tags for the payment screens. Android exposes them as resource ids
 * (`testTagsAsResourceId` in MainActivity), which is how the Appium tests in e2e/ find elements.
 */
object PaymentTestTags {
    const val RECIPIENT = "send_recipient"
    const val AMOUNT = "send_amount"
    const val CURRENCY = "send_currency"
    const val CURRENCY_OPTION_PREFIX = "send_currency_"
    const val SUBMIT = "send_submit"
    const val HISTORY_LIST = "history_list"
    const val HISTORY_ITEM = "history_item"
    const val HISTORY_EMPTY = "history_empty"
}
