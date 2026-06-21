package com.spexerise.watchapp.domain.readiness

object HrvComponent {
    fun score(todayRmssd: Float, baselineRmssd: Float): Float {
        if (baselineRmssd <= 0f) return 50f
        val ratio = todayRmssd / baselineRmssd
        return when {
            ratio >= 1.10f -> 100f
            ratio <= 0.80f -> 0f
            ratio >= 1.0f  -> ((ratio - 1.0f) / 0.10f) * 50f + 50f
            else           -> ((ratio - 0.80f) / 0.20f) * 50f
        }.coerceIn(0f, 100f)
    }
}
