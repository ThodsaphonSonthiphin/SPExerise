package com.spexerise.watchapp.domain.readiness

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spexerise.watchapp.data.db.AppDatabase
import com.spexerise.watchapp.data.db.ReadinessSnapshot
import com.spexerise.watchapp.data.sync.WearableSyncManager
import java.time.LocalDate

class ReadinessWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val thirtyDaysEpoch = LocalDate.now().minusDays(30).toEpochDay()

        // Compute ATL from last 7 days of workouts
        val recentWorkouts = db.workoutDao().getLast7Days(now - sevenDaysMs)
        val atl = AcuteTrainingLoad.compute(recentWorkouts)

        // Compute HRV baseline from last 30 days of snapshots
        val recentSnapshots = db.readinessDao().getLast30Days(thirtyDaysEpoch)
        val baselineRmssd = if (recentSnapshots.isEmpty()) 40f
            else recentSnapshots.map { it.hrvRmssd }.average().toFloat()
        // Use the most recent RMSSD as today's reading (updated by PassiveHrvSession separately)
        val todayRmssd = recentSnapshots.firstOrNull()?.hrvRmssd ?: baselineRmssd
        val todayRhr = recentSnapshots.firstOrNull()?.restingHrBpm ?: 60
        val baselineRhr = if (recentSnapshots.isEmpty()) 60f
            else recentSnapshots.map { it.restingHrBpm.toFloat() }.average().toFloat()

        // Placeholder sleep data — replace when Samsung Health SDK is integrated
        val sleepHours = 7.5f
        val sleepQuality = 0.7f

        // Compute component scores
        val hrvScore = HrvComponent.score(todayRmssd, baselineRmssd)
        val sleepScore = SleepComponent.score(sleepHours, sleepQuality)
        val rhrScore = RhrComponent.score(todayRhr, baselineRhr)

        // Compute final readiness score
        val score = ReadinessCalculator.compute(hrvScore, sleepScore, atl, rhrScore)

        val snapshot = ReadinessSnapshot(
            dateEpochDay = LocalDate.now().toEpochDay(),
            score = score,
            hrvRmssd = todayRmssd,
            sleepHours = sleepHours,
            sleepQualityScore = sleepQuality,
            atl = atl,
            restingHrBpm = todayRhr
        )

        db.readinessDao().insert(snapshot)
        WearableSyncManager.pushReadiness(applicationContext, snapshot)

        return Result.success()
    }
}
