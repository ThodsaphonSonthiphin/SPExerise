package com.spexerise.watchapp.data.db
import android.content.Context
import androidx.room.*

@Database(
    entities = [WorkoutRecord::class, ReadinessSnapshot::class, Vo2MaxRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun readinessDao(): ReadinessDao
    abstract fun vo2MaxDao(): Vo2MaxDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "spexerise.db"
                ).build().also { INSTANCE = it }
            }
    }
}
