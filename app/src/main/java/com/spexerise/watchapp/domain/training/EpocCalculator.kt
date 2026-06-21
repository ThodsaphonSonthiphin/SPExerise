package com.spexerise.watchapp.domain.training

data class EpocState(
    val accumulatedEpoc: Float,
    val anaerobicContribution: Float
)

object EpocCalculator {
    fun accumulate(
        state: EpocState,
        hrBpm: Int,
        hrRestBpm: Int,
        hrMaxBpm: Int,
        vo2Max: Float,
        deltaSeconds: Int
    ): EpocState {
        val hrReserve = (hrBpm - hrRestBpm).toFloat() / (hrMaxBpm - hrRestBpm)
        if (hrReserve <= 0f) return state

        val epocRatePerMin = when {
            hrReserve < 0.50f -> 0f
            hrReserve < 0.60f -> vo2Max * 0.003f * hrReserve
            hrReserve < 0.75f -> vo2Max * 0.008f * hrReserve
            hrReserve < 0.85f -> vo2Max * 0.018f * hrReserve
            else               -> vo2Max * 0.030f * hrReserve
        }

        val deltaMinutes = deltaSeconds / 60f
        val epocDelta = epocRatePerMin * deltaMinutes
        val anaerobicDelta = if (hrReserve > 0.85f) hrReserve * deltaMinutes else 0f

        return EpocState(
            accumulatedEpoc = state.accumulatedEpoc + epocDelta,
            anaerobicContribution = state.anaerobicContribution + anaerobicDelta
        )
    }
}
