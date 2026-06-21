package com.spexerise.watchapp.domain.training

object TrainingEffectScore {

    fun aerobic(state: EpocState, vo2Max: Float): Float {
        if (vo2Max <= 0f) return 0f
        val ratio = state.accumulatedEpoc / vo2Max
        return when {
            ratio <= 0f    -> 0.0f
            ratio <= 0.10f -> ratio / 0.10f * 1.0f
            ratio <= 0.25f -> 1.0f + (ratio - 0.10f) / 0.15f
            ratio <= 0.45f -> 2.0f + (ratio - 0.25f) / 0.20f
            ratio <= 0.70f -> 3.0f + (ratio - 0.45f) / 0.25f
            ratio <= 1.00f -> 4.0f + (ratio - 0.70f) / 0.30f
            else           -> 5.0f
        }.coerceIn(0.0f, 5.0f)
    }

    fun anaerobic(state: EpocState, vo2Max: Float): Float {
        val ratio = state.anaerobicContribution / (vo2Max * 0.1f).coerceAtLeast(1f)
        return (ratio * 5.0f).coerceIn(0.0f, 5.0f)
    }

    fun aerobicLabel(score: Float): String = when {
        score < 1.0f -> "No benefit"
        score < 2.0f -> "Recovery"
        score < 3.0f -> "Maintaining aerobic fitness"
        score < 4.0f -> "Improving aerobic base"
        score < 5.0f -> "Highly improving aerobic capacity"
        else         -> "Overreaching — rest soon"
    }
}
