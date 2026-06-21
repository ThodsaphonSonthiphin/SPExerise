package com.spexerise.watchapp.data.db
import androidx.room.*

@Dao
interface ReadinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(snapshot: ReadinessSnapshot)

    @Query("SELECT * FROM readiness_snapshots ORDER BY dateEpochDay DESC LIMIT 1")
    fun getLatest(): ReadinessSnapshot?

    @Query("SELECT * FROM readiness_snapshots WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun getLast30Days(sinceEpochDay: Long): List<ReadinessSnapshot>
}
