package com.asterixorobelix.remit.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Handles a ViewModel's one-off effects (its `Channel`) while the screen is at least STARTED.
 *
 * Why not `LaunchedEffect(viewModel) { viewModel.effects.collect { ... } }`? A composition stays
 * alive while its activity is stopped, so that collector keeps running in the background: it takes
 * an effect out of the channel and shows a snackbar (or navigates) while nobody can see it, and the
 * effect is gone when the user comes back. This is the same `LaunchedEffect` plus two things Google
 * recommends for collecting flows in UI:
 * - `repeatOnLifecycle(STARTED)` stops collecting when the screen is hidden and starts again when it
 *   returns, so an effect sent meanwhile waits in the channel until it can be seen.
 * - `Dispatchers.Main.immediate` handles an effect in the same main-thread turn it is taken from the
 *   channel, so stopping the screen cannot cancel the collector between taking and handling it.
 */
@Composable
fun <T> CollectEffects(
    effects: Flow<T>,
    onEffect: suspend (T) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                effects.collect { effect -> currentOnEffect(effect) }
            }
        }
    }
}
