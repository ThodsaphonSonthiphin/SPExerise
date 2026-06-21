package com.spexerise.watchapp.data.db
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vo2max_records")
data class Vo2MaxRecord(
    @PrimaryKey val measuredAtMs: Long,
    val vo2Max: Float,
    val source: String  // "samsung_health" | "calibration" | "manual"
)
