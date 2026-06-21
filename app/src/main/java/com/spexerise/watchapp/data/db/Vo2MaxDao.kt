package com.spexerise.watchapp.data.db
import androidx.room.*

@Dao
interface Vo2MaxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: Vo2MaxRecord)

    @Query("SELECT * FROM vo2max_records ORDER BY measuredAtMs DESC LIMIT 1")
    fun getLatest(): Vo2MaxRecord?
}
