package com.spexerise.watchapp.domain.readiness

object SleepComponent {
    fun score(hours: Float, qualityScore: Float): Float {
        val durationScore = when {
            hours < 5.0f  -> 0f
            hours <= 7.0f -> (hours - 5.0f) / 2.0f * 100f
            hours <= 9.0f -> 100f
            hours <= 10f  -> (10f - hours) / 1.0f * 100f
            else          -> 0f
        }
        return (durationScore * qualityScore.coerceIn(0f, 1f)).coerceIn(0f, 100f)
    }
}
