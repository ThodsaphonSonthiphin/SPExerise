package com.spexerise.watchapp.data.db
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readiness_snapshots")
data class ReadinessSnapshot(
    @PrimaryKey val dateEpochDay: Long,
    val score: Int,
    val hrvRmssd: Float,
    val sleepHours: Float,
    val sleepQualityScore: Float,
    val atl: Float,
    val restingHrBpm: Int
)
