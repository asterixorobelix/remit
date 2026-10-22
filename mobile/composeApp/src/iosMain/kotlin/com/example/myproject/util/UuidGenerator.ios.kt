package com.example.myproject.util

import platform.Foundation.NSUUID

/**
 * iOS implementation using NSUUID.
 */
actual fun generateId(): String = NSUUID().UUIDString()
