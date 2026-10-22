package com.asterixorobelix.remit.util

import java.util.UUID

/**
 * Android implementation using java.util.UUID.
 */
actual fun generateId(): String = UUID.randomUUID().toString()
