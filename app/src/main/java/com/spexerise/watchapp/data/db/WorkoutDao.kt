package com.spexerise.watchapp.data.db
import androidx.room.*

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: WorkoutRecord)

    @Query("SELECT * FROM workout_records WHERE startTimeMs >= :sinceMs ORDER BY startTimeMs DESC")
    fun getLast7Days(sinceMs: Long): List<WorkoutRecord>
}
