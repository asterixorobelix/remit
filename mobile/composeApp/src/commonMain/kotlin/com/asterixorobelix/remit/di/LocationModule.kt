package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.platform.LocationService
import com.asterixorobelix.remit.platform.createLocationService
import org.koin.dsl.module

/**
 * Koin module for location service dependencies.
 *
 * Provides the platform-specific [LocationService] implementation
 * for GPS location tracking on the map.
 */
val locationModule = module {
    single<LocationService> { createLocationService() }
}
