package com.spexerise.watchapp.data.health

import android.content.Context
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await

object ExerciseRepository {

    fun hrFlow(context: Context): Flow<Int> = callbackFlow {
        val client = HealthServices.getClient(context).exerciseClient
        val config = ExerciseConfig.builder(ExerciseType.RUNNING)
            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
            .setIsAutoPauseAndResumeEnabled(false)
            .build()

        val callback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                update.latestMetrics.getData(DataType.HEART_RATE_BPM).forEach { point ->
                    trySend(point.value.toInt())
                }
            }
            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
            override fun onRegistered() {}
            override fun onRegistrationFailed(throwable: Throwable) { close(throwable) }
            override fun onAvailabilityChanged(
                dataType: androidx.health.services.client.data.DataType<*, *>,
                availability: androidx.health.services.client.data.Availability
            ) {}
        }

        client.setUpdateCallback(callback)
        runCatching { client.startExerciseAsync(config).await() }

        awaitClose {
            runCatching { client.endExerciseAsync() }
            client.clearUpdateCallbackAsync(callback)
        }
    }
}
