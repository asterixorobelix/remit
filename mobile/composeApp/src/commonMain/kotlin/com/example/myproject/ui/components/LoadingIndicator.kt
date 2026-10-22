package com.example.myproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_loading_indicator
import com.example.myproject.generated.resources.message_loading
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * A circular loading indicator with optional loading message.
 *
 * Uses Material Design 3 styling and proper accessibility semantics.
 * The indicator size and color adapt to the current theme.
 *
 * @param modifier Modifier to be applied to the container
 * @param message Optional message to display below the indicator
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    val loadingDescription = stringResource(Res.string.cd_loading_indicator)

    Column(
        modifier = modifier
            .semantics { contentDescription = loadingDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.touchTargetMinimum),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        if (message != null) {
            Text(
                text = message,
                modifier = Modifier.padding(top = Dimens.spacingMedium),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full-screen loading state that centers the indicator.
 *
 * Use this when loading is the primary state of a screen.
 *
 * @param modifier Modifier to be applied to the container
 * @param message Optional message to display below the indicator
 */
@Composable
fun FullScreenLoading(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator(message = message)
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun LoadingIndicatorPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Basic loading indicator
            LoadingIndicator()

            // Loading indicator with message
            LoadingIndicator(message = stringResource(Res.string.message_loading))
        }
    }
}
