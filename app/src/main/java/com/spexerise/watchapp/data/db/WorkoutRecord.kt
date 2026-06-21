package com.spexerise.watchapp.data.db
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey val id: String,
    val startTimeMs: Long,
    val durationSeconds: Int,
    val avgHrBpm: Int,
    val trainingStressScore: Float,
    val aerobicTE: Float,
    val anaerobicTE: Float,
    val peakEpoc: Float
)
