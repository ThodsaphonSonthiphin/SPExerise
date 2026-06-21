package com.spexerise.watchapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.spexerise.watchapp.domain.readiness.ReadinessWorker
import java.util.concurrent.TimeUnit

class WatchApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleReadinessWorker()
    }

    private fun scheduleReadinessWorker() {
        val request = PeriodicWorkRequestBuilder<ReadinessWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "readiness_daily",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
