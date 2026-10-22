package com.asterixorobelix.remit.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens

/**
 * Shows a screen once per state, side by side in phone-sized frames, so a screen file's one
 * `@Preview` covers every state:
 *
 * ```
 * ScreenStates(listOf(UiState.Loading, UiState.Empty, UiState.Content(items))) { state ->
 *     MyContent(state = state, onEvent = {})
 * }
 * ```
 *
 * The frames have a fixed size because screens fill and scroll the space they are given.
 */
@Composable
internal fun <T> ScreenStates(states: List<T>, content: @Composable (T) -> Unit) {
    AppTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.outlineVariant)
                .padding(Dimens.spacingSmall),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            states.forEach { state ->
                Surface(modifier = Modifier.size(Dimens.previewFrameWidth, Dimens.previewFrameHeight)) {
                    content(state)
                }
            }
        }
    }
}

/** A screen with a single state, framed like [ScreenStates]. */
@Composable
internal fun ScreenStates(content: @Composable () -> Unit) {
    ScreenStates(listOf(Unit)) { content() }
}
