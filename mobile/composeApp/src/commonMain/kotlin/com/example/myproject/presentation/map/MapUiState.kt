package com.example.myproject.presentation.map

import com.example.myproject.platform.PermissionState
import org.jetbrains.compose.resources.StringResource

/**
 * UI state for the map screen.
 *
 * Represents the different states the map can be in:
 * - Loading: Initial state while map resources are being prepared
 * - Content: Map is ready with camera position and user location
 * - Error: Something went wrong loading the map
 */
sealed interface MapUiState {
    /**
     * Map is loading/initializing.
     */
    data object Loading : MapUiState

    /**
     * Map is ready and displaying.
     *
     * @param centerLatitude The latitude of the map center
     * @param centerLongitude The longitude of the map center
     * @param zoomLevel The current zoom level
     * @param userLatitude The user's current GPS latitude (null if not tracking)
     * @param userLongitude The user's current GPS longitude (null if not tracking)
     * @param locationPermissionState The current location permission state
     * @param isTrackingLocation Whether location tracking is currently active
     * @param centerOnUserLocationRequest Incremented when user requests centering on their location
     */
    data class Content(
        val centerLatitude: Double = DEFAULT_CENTER_LATITUDE,
        val centerLongitude: Double = DEFAULT_CENTER_LONGITUDE,
        val zoomLevel: Double = DEFAULT_ZOOM_LEVEL,
        val userLatitude: Double? = null,
        val userLongitude: Double? = null,
        val locationPermissionState: PermissionState = PermissionState.NOT_DETERMINED,
        val isTrackingLocation: Boolean = false,
        val centerOnUserLocationRequest: Int = 0,
    ) : MapUiState {
        /** True if we have valid user location coordinates */
        val hasUserLocation: Boolean
            get() = userLatitude != null && userLongitude != null
    }

    /**
     * An error occurred while loading the map.
     *
     * @param message The error message to display
     */
    data class Error(val message: StringResource) : MapUiState

    companion object {
        // Integration note: Replace with your project's default map center
        const val DEFAULT_CENTER_LATITUDE = 0.0
        const val DEFAULT_CENTER_LONGITUDE = 0.0
        const val DEFAULT_ZOOM_LEVEL = 12.0
    }
}
