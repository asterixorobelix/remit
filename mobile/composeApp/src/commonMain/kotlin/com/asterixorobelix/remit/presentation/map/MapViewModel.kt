package com.asterixorobelix.remit.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.platform.CrashLogger
import com.asterixorobelix.remit.platform.LocationService
import com.asterixorobelix.remit.platform.PermissionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the map screen.
 *
 * Manages camera position state, handles map interactions,
 * and provides location tracking for user position display.
 */
class MapViewModel(
    private val locationService: LocationService,
    private val crashLogger: CrashLogger,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Content())

    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Location updates are collected only while this is true (no mutable Job handle needed). */
    private val trackingRequested = MutableStateFlow(false)

    init {
        observeLocationUpdates()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLocationUpdates() {
        viewModelScope.launch {
            trackingRequested
                .flatMapLatest { tracking ->
                    if (tracking) {
                        locationService.startLocationUpdates()
                            .catch { throwable -> crashLogger.logException(throwable, "Location tracking failed") }
                    } else {
                        emptyFlow()
                    }
                }
                .collect { location -> updateUserLocation(location.latitude, location.longitude) }
        }
    }

    /**
     * Called when the camera position changes due to user interaction.
     */
    fun onCameraPositionChanged(latitude: Double, longitude: Double, zoom: Double) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Content) {
            _uiState.value = currentState.copy(
                centerLatitude = latitude,
                centerLongitude = longitude,
                zoomLevel = zoom,
            )
        }
    }

    /**
     * Check location permission state without requesting.
     * If permission is already granted, starts location tracking.
     * If NOT_DETERMINED, the UI layer should trigger the permission request.
     */
    fun checkLocationPermission() {
        viewModelScope.launch {
            val permission = locationService.checkPermission()
            updatePermissionState(permission)

            if (permission == PermissionState.GRANTED) {
                startLocationTracking()
            }
        }
    }

    /**
     * Handle the result from the UI-level permission request.
     *
     * @param granted True if location permission was granted
     */
    fun onPermissionResult(granted: Boolean) {
        val newState = if (granted) PermissionState.GRANTED else PermissionState.DENIED
        updatePermissionState(newState)

        if (granted) {
            startLocationTracking()
        }
    }

    /**
     * Center map on user's current location.
     * Increments the counter to trigger camera animation in the UI layer.
     */
    fun centerOnUserLocation() {
        val currentState = _uiState.value
        if (currentState is MapUiState.Content && currentState.hasUserLocation) {
            _uiState.value = currentState.copy(
                centerOnUserLocationRequest = currentState.centerOnUserLocationRequest + 1,
            )
        }
    }

    /**
     * Stop location tracking to conserve battery.
     */
    fun stopLocationTracking() {
        trackingRequested.value = false
        locationService.stopLocationUpdates()
        updateTrackingState(isTracking = false)
    }

    /**
     * Resets the map to its initial state with default coordinates.
     */
    fun resetToDefaultPosition() {
        _uiState.value = MapUiState.Content()
    }

    private fun startLocationTracking() {
        trackingRequested.value = true
        updateTrackingState(isTracking = true)
    }

    private fun updateUserLocation(lat: Double, lng: Double) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Content) {
            _uiState.value = currentState.copy(
                userLatitude = lat,
                userLongitude = lng,
            )
        }
    }

    private fun updatePermissionState(state: PermissionState) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Content) {
            _uiState.value = currentState.copy(locationPermissionState = state)
        }
    }

    private fun updateTrackingState(isTracking: Boolean) {
        val currentState = _uiState.value
        if (currentState is MapUiState.Content) {
            _uiState.value = currentState.copy(isTrackingLocation = isTracking)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationTracking()
    }
}
