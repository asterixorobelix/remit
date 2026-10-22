package com.example.myproject.util

import java.util.UUID

actual fun generateId(): String = UUID.randomUUID().toString()
