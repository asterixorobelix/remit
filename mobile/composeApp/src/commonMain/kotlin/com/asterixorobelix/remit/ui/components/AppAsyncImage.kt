package com.asterixorobelix.remit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.asterixorobelix.remit.ui.theme.Dimens

/**
 * Reusable async image component powered by Coil 3.
 *
 * Requires the following Gradle dependencies:
 * ```toml
 * # libs.versions.toml
 * coil = "3.x.x"
 *
 * [libraries]
 * coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
 * coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
 * ```
 *
 * Three variants are provided:
 * - [AppAsyncImage]          – Full subcomposition with built-in loading/error UI
 * - [AppAsyncImageSimple]    – Lightweight version using Painter placeholders
 * - [AppAsyncImageWithState] – Observe loading state changes via callback
 */

private const val DEFAULT_CROSSFADE_DURATION_MS = 300

/**
 * Async image with built-in loading spinner and error state.
 *
 * Renders a [CircularProgressIndicator] while loading and a broken-image
 * icon if the load fails. Uses Coil's subcomposition — slightly heavier
 * than [AppAsyncImageSimple] but more flexible for custom loading UI.
 *
 * Usage:
 * ```kotlin
 * AppAsyncImage(
 *     model = "https://example.com/photo.jpg",
 *     contentDescription = "User avatar",
 *     modifier = Modifier.size(64.dp).clip(CircleShape)
 * )
 * ```
 *
 * @param model               Image source (URL string, URI, File, drawable res, etc.)
 * @param contentDescription  Accessibility description
 * @param modifier            Applied to the image container
 * @param contentScale        How to scale the image within its bounds (default: Crop)
 * @param errorIcon           Icon shown on load failure (default: BrokenImage)
 * @param crossfadeDurationMs Crossfade duration on successful load (default: 300 ms)
 * @param colorFilter         Optional colour filter applied to the image
 */
@Suppress("LongParameterList")
@Composable
fun AppAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    errorIcon: ImageVector = Icons.Default.BrokenImage,
    crossfadeDurationMs: Int = DEFAULT_CROSSFADE_DURATION_MS,
    colorFilter: ColorFilter? = null,
) {
    val context = LocalPlatformContext.current

    val imageRequest = ImageRequest.Builder(context)
        .data(model)
        .crossfade(crossfadeDurationMs)
        .build()

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
        loading = {
            ImageLoadingPlaceholder(modifier = Modifier.matchParentSize())
        },
        error = {
            ImageErrorPlaceholder(
                icon = errorIcon,
                modifier = Modifier.matchParentSize(),
            )
        },
    )
}

/**
 * Lightweight async image using Painter placeholders (no subcomposition).
 *
 * Prefer this over [AppAsyncImage] in performance-sensitive lists where you
 * already have placeholder drawables available.
 *
 * Usage:
 * ```kotlin
 * AppAsyncImageSimple(
 *     model = item.thumbnailUrl,
 *     contentDescription = item.name,
 *     placeholder = painterResource(Res.drawable.img_placeholder),
 *     error = painterResource(Res.drawable.img_error),
 *     modifier = Modifier.fillMaxWidth().height(200.dp)
 * )
 * ```
 *
 * @param model               Image source
 * @param contentDescription  Accessibility description
 * @param modifier            Applied to the image
 * @param contentScale        How to scale the image
 * @param placeholder         Painter shown while loading
 * @param error               Painter shown on load failure
 * @param crossfadeDurationMs Crossfade on success
 * @param colorFilter         Optional colour filter
 */
@Suppress("LongParameterList")
@Composable
fun AppAsyncImageSimple(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Painter? = null,
    error: Painter? = null,
    crossfadeDurationMs: Int = DEFAULT_CROSSFADE_DURATION_MS,
    colorFilter: ColorFilter? = null,
) {
    val context = LocalPlatformContext.current

    val imageRequest = ImageRequest.Builder(context)
        .data(model)
        .crossfade(crossfadeDurationMs)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = placeholder,
        error = error,
        colorFilter = colorFilter,
    )
}

/**
 * Async image with a state-observation callback.
 *
 * Use when you need to react to load state changes — for example to show
 * a custom loading indicator outside the image bounds, or to log errors.
 *
 * Usage:
 * ```kotlin
 * AppAsyncImageWithState(
 *     model = user.avatarUrl,
 *     contentDescription = "Avatar",
 *     onState = { state ->
 *         if (state is AsyncImagePainter.State.Error) logError(state.result.throwable)
 *     },
 *     modifier = Modifier.size(48.dp).clip(CircleShape)
 * )
 * ```
 *
 * @param model               Image source
 * @param contentDescription  Accessibility description
 * @param modifier            Applied to the image
 * @param contentScale        How to scale the image
 * @param crossfadeDurationMs Crossfade on success
 * @param colorFilter         Optional colour filter
 * @param onState             Callback for [AsyncImagePainter.State] changes
 */
@Suppress("LongParameterList")
@Composable
fun AppAsyncImageWithState(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    crossfadeDurationMs: Int = DEFAULT_CROSSFADE_DURATION_MS,
    colorFilter: ColorFilter? = null,
    onState: ((AsyncImagePainter.State) -> Unit)? = null,
) {
    val context = LocalPlatformContext.current

    val imageRequest = ImageRequest.Builder(context)
        .data(model)
        .crossfade(crossfadeDurationMs)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = colorFilter,
        onState = onState,
    )
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

@Composable
private fun ImageLoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.iconSizeMedium),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = Dimens.cornerRadiusExtraSmall,
        )
    }
}

@Composable
private fun ImageErrorPlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeLarge),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
