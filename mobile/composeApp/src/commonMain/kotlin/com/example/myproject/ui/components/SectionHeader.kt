package com.example.myproject.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myproject.ui.theme.Dimens

/**
 * Section header used across the app for grouping related content.
 *
 * Uses primary color and label typography following Material Design 3
 * guidelines for list section headers.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            horizontal = Dimens.paddingMedium,
            vertical = Dimens.paddingSmall,
        ),
    )
}
