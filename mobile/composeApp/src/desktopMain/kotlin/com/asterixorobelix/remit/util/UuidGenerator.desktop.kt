package com.asterixorobelix.remit.util

import java.util.UUID

actual fun generateId(): String = UUID.randomUUID().toString()
