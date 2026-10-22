package com.asterixorobelix.remit.gallery

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.gallery_primary_action
import com.asterixorobelix.remit.generated.resources.gallery_status_label
import com.asterixorobelix.remit.generated.resources.gallery_title
import com.asterixorobelix.remit.ui.components.BadgeStatus
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.components.StatusBadge
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = stringResource(Res.string.gallery_title)) { ComponentGallery() }
}

/**
 * Fast shared-UI feedback loop (`make ui`); add components here while building them. The
 * desktop screenshot tests capture it in both themes.
 */
@Composable
fun ComponentGallery(
    modifier: Modifier = Modifier,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
) = AppTheme(useDarkTheme = useDarkTheme) {
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier.padding(Dimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            Text(stringResource(Res.string.gallery_title), style = MaterialTheme.typography.headlineMedium)
            PrimaryButton(onClick = {}, text = stringResource(Res.string.gallery_primary_action))
            StatusBadge(status = BadgeStatus.Success, label = stringResource(Res.string.gallery_status_label))
        }
    }
}
