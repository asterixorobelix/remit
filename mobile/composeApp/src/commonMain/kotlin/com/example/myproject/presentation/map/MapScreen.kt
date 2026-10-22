package com.example.myproject.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.button_retry
import com.example.myproject.generated.resources.cd_navigate_back
import com.example.myproject.generated.resources.map_error_title
import com.example.myproject.generated.resources.map_loading
import com.example.myproject.generated.resources.screen_map_title
import com.example.myproject.platform.PermissionState
import com.example.myproject.platform.rememberLocationPermissionLauncher
import com.example.myproject.ui.theme.Dimens
import io.github.dellisd.spatialk.geojson.Point
import io.github.dellisd.spatialk.geojson.Position
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import kotlin.time.Duration.Companion.milliseconds
import io.github.dellisd.spatialk.geojson.Feature as GeoJsonFeature

// Integration note: Replace with your map style URL (e.g. MapTiler, Stadia Maps, or local PMTiles style.json)
private const val MAP_STYLE_URL = "https://demotiles.maplibre.org/style.json"

private const val LOCATION_HALO_ALPHA = 0.2f

/**
 * Main map screen composable.
 *
 * Displays an interactive MapLibre map with user GPS location
 * as a blue dot and a My Location FAB to center on position.
 *
 * ## Customization
 * - Replace [MAP_STYLE_URL] with your map style (local PMTiles or remote)
 * - Add marker layers by adding [CircleLayer] composables inside the MaplibreMap content
 * - Modify [MapUiState.DEFAULT_CENTER_LATITUDE] / [MapUiState.DEFAULT_CENTER_LONGITUDE] for your region
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val requestPermission = rememberLocationPermissionLauncher { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkLocationPermission()
    }

    val currentState = uiState
    LaunchedEffect(currentState) {
        if (currentState is MapUiState.Content &&
            currentState.locationPermissionState == PermissionState.NOT_DETERMINED
        ) {
            requestPermission()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopLocationTracking()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is MapUiState.Loading -> MapLoadingContent()

                is MapUiState.Content -> MapContent(
                    state = state,
                    onCenterOnUser = viewModel::centerOnUserLocation,
                )

                is MapUiState.Error -> MapErrorContent(
                    message = stringResource(state.message),
                    onRetry = viewModel::resetToDefaultPosition,
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    state: MapUiState.Content,
    onCenterOnUser: () -> Unit,
) {
    val cameraState = rememberCameraState(
        CameraPosition(
            target = Position(state.centerLongitude, state.centerLatitude),
            zoom = state.zoomLevel,
        ),
    )

    // Animate camera when user requests centering on their location
    val userLng = state.userLongitude
    val userLat = state.userLatitude
    LaunchedEffect(state.centerOnUserLocationRequest) {
        if (state.centerOnUserLocationRequest > 0 && userLng != null && userLat != null) {
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = Position(longitude = userLng, latitude = userLat),
                ),
                duration = Dimens.ANIMATION_DURATION_LONG.milliseconds,
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(MAP_STYLE_URL),
            cameraState = cameraState,
        ) {
            // User location blue dot
            UserLocationLayer(state = state)

            // Integration note: Add your own marker layers here
            // Example:
            // val source = rememberGeoJsonSource(data = GeoJsonData.Uri(Res.getUri("files/maps/markers.geojson")))
            // CircleLayer(id = "markers", source = source, color = const(Color.Red), radius = const(10.dp))
        }

        if (state.locationPermissionState == PermissionState.GRANTED) {
            MyLocationButton(
                onClick = onCenterOnUser,
                enabled = state.hasUserLocation,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Dimens.paddingMedium, bottom = Dimens.paddingExtraLarge),
            )
        }
    }
}

@Composable
private fun UserLocationLayer(state: MapUiState.Content) {
    val locLng = state.userLongitude
    val locLat = state.userLatitude
    val dotColor = MaterialTheme.colorScheme.primary
    val haloColor = dotColor.copy(alpha = LOCATION_HALO_ALPHA)
    val strokeColor = MaterialTheme.colorScheme.surface
    if (locLng != null && locLat != null) {
        val userLocationFeature = GeoJsonFeature(
            geometry = Point(coordinates = Position(longitude = locLng, latitude = locLat)),
        )
        val userLocationSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(userLocationFeature),
        )
        CircleLayer(
            id = "user-location-halo",
            source = userLocationSource,
            color = const(haloColor),
            radius = const(Dimens.mapLocationHaloRadius),
        )
        CircleLayer(
            id = "user-location",
            source = userLocationSource,
            color = const(dotColor),
            radius = const(Dimens.mapLocationDotRadius),
            strokeColor = const(strokeColor),
            strokeWidth = const(Dimens.mapLocationStrokeWidth),
        )
    }
}

@Composable
private fun MapLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.iconSizeHero),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text(
                text = stringResource(Res.string.map_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun MapErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimens.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Dimens.iconSizeHero),
        )

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        Text(
            text = stringResource(Res.string.map_error_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeSmall),
            )
            Spacer(modifier = Modifier.size(Dimens.spacingSmall))
            Text(
                text = stringResource(Res.string.button_retry),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
