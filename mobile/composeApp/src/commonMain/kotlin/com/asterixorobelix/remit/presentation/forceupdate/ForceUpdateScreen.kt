package com.asterixorobelix.remit.presentation.forceupdate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.force_update_button
import com.asterixorobelix.remit.generated.resources.force_update_message
import com.asterixorobelix.remit.generated.resources.force_update_title
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.theme.Dimens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val CONTENT_DELAY_MS = 200L

@Composable
fun ForceUpdateScreen(
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showIcon by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showIcon = true
        delay(CONTENT_DELAY_MS)
        showContent = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = showIcon,
            enter = scaleIn(
                spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
            ) + fadeIn(),
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeDisplayLarge),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn() + slideInVertically { it / 2 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.force_update_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                Text(
                    text = stringResource(Res.string.force_update_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

                PrimaryButton(
                    onClick = onUpdateClick,
                    text = stringResource(Res.string.force_update_button),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
