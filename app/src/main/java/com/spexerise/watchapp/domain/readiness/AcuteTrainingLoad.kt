package com.spexerise.watchapp.domain.readiness

import com.spexerise.watchapp.data.db.WorkoutRecord

object AcuteTrainingLoad {
    fun compute(workouts: List<WorkoutRecord>): Float {
        val lambda = 0.25f
        val sorted = workouts.sortedBy { it.startTimeMs }
        var ewma = 0f
        for (w in sorted) {
            ewma = lambda * w.trainingStressScore + (1f - lambda) * ewma
        }
        return ewma
    }
}
