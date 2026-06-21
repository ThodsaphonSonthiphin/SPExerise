package com.spexerise.watchapp.domain.readiness

object ReadinessCalculator {
    fun compute(hrv: Float, sleep: Float, atl: Float, rhr: Float): Int {
        val atlComponent = (1f - (atl / 100f).coerceIn(0f, 1f)) * 100f
        val raw = hrv * 0.35f + sleep * 0.30f + atlComponent * 0.20f + rhr * 0.15f
        return raw.toInt().coerceIn(0, 100)
    }

    fun label(score: Int): String = when {
        score >= 80 -> "Primed"
        score >= 60 -> "Ready"
        score >= 40 -> "Moderate"
        score >= 20 -> "Low"
        else        -> "Rest"
    }
}
