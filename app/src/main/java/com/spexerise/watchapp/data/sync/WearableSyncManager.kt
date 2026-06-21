package com.spexerise.watchapp.data.sync

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.spexerise.watchapp.data.db.ReadinessSnapshot
import com.spexerise.watchapp.data.db.WorkoutRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object WearableSyncManager {
    fun pushWorkout(context: Context, record: WorkoutRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val request = PutDataMapRequest.create("/workout/${record.id}").apply {
                    dataMap.putLong("startTimeMs", record.startTimeMs)
                    dataMap.putInt("durationSeconds", record.durationSeconds)
                    dataMap.putInt("avgHrBpm", record.avgHrBpm)
                    dataMap.putFloat("trainingStressScore", record.trainingStressScore)
                    dataMap.putFloat("aerobicTE", record.aerobicTE)
                    dataMap.putFloat("anaerobicTE", record.anaerobicTE)
                    dataMap.putFloat("peakEpoc", record.peakEpoc)
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(request).await()
            }
        }
    }

    fun pushReadiness(context: Context, snapshot: ReadinessSnapshot) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val request = PutDataMapRequest.create("/readiness/${snapshot.dateEpochDay}").apply {
                    dataMap.putInt("score", snapshot.score)
                    dataMap.putFloat("hrvRmssd", snapshot.hrvRmssd)
                    dataMap.putFloat("sleepHours", snapshot.sleepHours)
                    dataMap.putFloat("sleepQualityScore", snapshot.sleepQualityScore)
                    dataMap.putFloat("atl", snapshot.atl)
                    dataMap.putInt("restingHrBpm", snapshot.restingHrBpm)
                }.asPutDataRequest().setUrgent()
                Wearable.getDataClient(context).putDataItem(request).await()
            }
        }
    }
}
