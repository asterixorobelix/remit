package com.asterixorobelix.remit.testing

import app.cash.turbine.ReceiveTurbine

/** Skips emissions (for example an initial Loading) until one of type [R] arrives. */
suspend inline fun <reified R> ReceiveTurbine<*>.awaitItemOfType(): R {
    while (true) {
        val item = awaitItem()
        if (item is R) return item
    }
}
