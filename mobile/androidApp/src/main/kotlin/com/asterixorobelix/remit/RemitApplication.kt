package com.asterixorobelix.remit

import android.app.Application

class RemitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startRemit(AndroidAppConfig())
    }
}
