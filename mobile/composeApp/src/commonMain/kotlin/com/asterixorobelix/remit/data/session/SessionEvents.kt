package com.asterixorobelix.remit.data.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-process signal raised by the network layer when the server revokes the session. */
class SessionEvents {
    private val expiredEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val expired: SharedFlow<Unit> = expiredEvents.asSharedFlow()

    fun notifyExpired() {
        expiredEvents.tryEmit(Unit)
    }
}
