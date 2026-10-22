package com.example.myproject.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Dispatchers used by repositories so database and file work never runs on Main.
 * Tests substitute a test dispatcher through this interface.
 */
interface AppDispatchers {
    val io: CoroutineDispatcher
}

class DefaultAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
}
