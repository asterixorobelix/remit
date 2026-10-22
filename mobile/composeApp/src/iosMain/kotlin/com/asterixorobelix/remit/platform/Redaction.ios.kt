package com.asterixorobelix.remit.platform

/** Kotlin/Native cannot replace a stack trace; the redacted copy keeps its own. */
internal actual fun Throwable.copyStackTraceTo(target: Throwable) = Unit
