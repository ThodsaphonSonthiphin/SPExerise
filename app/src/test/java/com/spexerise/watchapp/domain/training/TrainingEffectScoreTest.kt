package com.spexerise.watchapp.domain.training

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrainingEffectScoreTest {

    @Test fun `zero epoc gives aerobic score 0`() {
        val score = TrainingEffectScore.aerobic(EpocState(0f, 0f), 50f)
        assertThat(score).isEqualTo(0.0f)
    }

    @Test fun `aerobic label for 2_3 is Maintaining`() {
        assertThat(TrainingEffectScore.aerobicLabel(2.3f)).isEqualTo("Maintaining aerobic fitness")
    }

    @Test fun `aerobic label for 3_5 is Improving`() {
        assertThat(TrainingEffectScore.aerobicLabel(3.5f)).isEqualTo("Improving aerobic base")
    }

    @Test fun `aerobic score is capped at 5`() {
        val state = EpocState(accumulatedEpoc = 1000f, anaerobicContribution = 0f)
        val score = TrainingEffectScore.aerobic(state, 50f)
        assertThat(score).isEqualTo(5.0f)
    }

    @Test fun `anaerobic score is 0 when no anaerobic contribution`() {
        val state = EpocState(0f, 0f)
        val score = TrainingEffectScore.anaerobic(state, 50f)
        assertThat(score).isEqualTo(0.0f)
    }
}
