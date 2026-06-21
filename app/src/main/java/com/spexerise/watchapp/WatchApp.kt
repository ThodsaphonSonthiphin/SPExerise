package com.spexerise.watchapp

import android.app.Application

/**
 * Application class for SPExerise Wear OS app.
 * Initializes app-wide components such as DI and logging.
 */
class WatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Future: initialize Hilt, logging, or other app-wide components here
    }
}
