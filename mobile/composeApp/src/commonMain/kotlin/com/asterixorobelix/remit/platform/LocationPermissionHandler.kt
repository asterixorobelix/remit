package com.asterixorobelix.remit.platform

import androidx.compose.runtime.Composable

/**
 * Remembers and provides a location permission handler.
 *
 * On Android, this uses ActivityResultContracts.RequestMultiplePermissions
 * to show the system permission dialog.
 *
 * On iOS, this delegates to CLLocationManager.requestWhenInUseAuthorization().
 *
 * @param onResult Callback invoked when permission is granted or denied
 * @return A function that triggers the permission request
 */
@Composable
expect fun rememberLocationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit
