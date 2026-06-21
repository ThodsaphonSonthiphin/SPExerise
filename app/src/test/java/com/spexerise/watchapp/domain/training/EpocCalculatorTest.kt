package com.spexerise.watchapp.domain.training

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EpocCalculatorTest {

    @Test fun `epoc accumulates at zone 3 heart rate`() {
        val state = EpocState(0f, 0f)
        val hr = 55 + (0.70f * (185 - 55)).toInt()  // 146 bpm
        val result = EpocCalculator.accumulate(state, hr, 55, 185, 50f, 60)
        assertThat(result.accumulatedEpoc).isGreaterThan(0f)
    }

    @Test fun `epoc is zero at rest heart rate`() {
        val state = EpocState(0f, 0f)
        val result = EpocCalculator.accumulate(state, 55, 55, 185, 50f, 60)
        assertThat(result.accumulatedEpoc).isEqualTo(0f)
    }

    @Test fun `30 minute zone 3 run produces aerobic TE between 2 and 4`() {
        var state = EpocState(0f, 0f)
        val hr = 146
        repeat(30 * 60 / 5) {
            state = EpocCalculator.accumulate(state, hr, 55, 185, 50f, 5)
        }
        val score = TrainingEffectScore.aerobic(state, 50f)
        assertThat(score).isAtLeast(2.0f)
        assertThat(score).isAtMost(4.0f)
    }

    @Test fun `epoc does not accumulate below 50 percent hr reserve`() {
        val state = EpocState(0f, 0f)
        val hr = 55 + (0.49f * (185 - 55)).toInt()  // just below 50% HR reserve
        val result = EpocCalculator.accumulate(state, hr, 55, 185, 50f, 60)
        assertThat(result.accumulatedEpoc).isEqualTo(0f)
    }
}
