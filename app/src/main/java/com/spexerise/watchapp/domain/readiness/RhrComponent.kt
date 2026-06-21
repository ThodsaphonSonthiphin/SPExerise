package com.spexerise.watchapp.domain.readiness

object RhrComponent {
    fun score(todayRhr: Int, baselineRhr: Float): Float {
        if (baselineRhr <= 0f) return 50f
        val diff = todayRhr - baselineRhr
        return when {
            diff <= -2f -> 100f
            diff >= 5f  -> 0f
            else        -> ((5f - diff) / 7f) * 100f
        }.coerceIn(0f, 100f)
    }
}
