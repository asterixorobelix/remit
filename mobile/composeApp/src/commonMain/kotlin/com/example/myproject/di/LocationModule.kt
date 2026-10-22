package com.example.myproject.di

import com.example.myproject.platform.LocationService
import com.example.myproject.platform.createLocationService
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
