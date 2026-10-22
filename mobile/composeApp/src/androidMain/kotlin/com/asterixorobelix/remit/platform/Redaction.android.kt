package com.asterixorobelix.remit.platform

internal actual fun Throwable.copyStackTraceTo(target: Throwable) {
    target.stackTrace = stackTrace
}
