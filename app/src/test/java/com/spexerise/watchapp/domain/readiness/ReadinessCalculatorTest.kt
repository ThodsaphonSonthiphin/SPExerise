package com.spexerise.watchapp.domain.readiness

import com.google.common.truth.Truth.assertThat
import com.spexerise.watchapp.data.db.WorkoutRecord
import org.junit.Test

class ReadinessCalculatorTest {

    @Test fun `perfect signals yield score 100`() {
        val score = ReadinessCalculator.compute(hrv = 100f, sleep = 100f, atl = 0f, rhr = 100f)
        assertThat(score).isEqualTo(100)
    }

    @Test fun `all zero components yield score 0`() {
        // atl=100 makes atlComponent=0, so all four components are 0
        val score = ReadinessCalculator.compute(hrv = 0f, sleep = 0f, atl = 100f, rhr = 0f)
        assertThat(score).isEqualTo(0)
    }

    @Test fun `hrv below baseline 20 percent gives component 0`() {
        val s = HrvComponent.score(todayRmssd = 32f, baselineRmssd = 40f)
        assertThat(s).isEqualTo(0f)
    }

    @Test fun `hrv at baseline gives component 50`() {
        val s = HrvComponent.score(todayRmssd = 40f, baselineRmssd = 40f)
        assertThat(s).isEqualTo(50f)
    }

    @Test fun `hrv 10 percent above baseline gives component 100`() {
        val s = HrvComponent.score(todayRmssd = 44f, baselineRmssd = 40f)
        assertThat(s).isEqualTo(100f)
    }

    @Test fun `7h30m sleep with perfect quality gives 100`() {
        val s = SleepComponent.score(hours = 7.5f, qualityScore = 1.0f)
        assertThat(s).isEqualTo(100f)
    }

    @Test fun `5h sleep gives component 0`() {
        val s = SleepComponent.score(hours = 5.0f, qualityScore = 1.0f)
        assertThat(s).isEqualTo(0f)
    }

    @Test fun `rhr 2 below baseline gives component 100`() {
        val s = RhrComponent.score(todayRhr = 58, baselineRhr = 60f)
        assertThat(s).isEqualTo(100f)
    }

    @Test fun `rhr 5 above baseline gives component 0`() {
        val s = RhrComponent.score(todayRhr = 65, baselineRhr = 60f)
        assertThat(s).isEqualTo(0f)
    }

    @Test fun `atl ewma increases with each workout`() {
        val workouts = listOf(
            WorkoutRecord("1", 1000L, 1800, 145, 60f, 3f, 0f, 30f),
            WorkoutRecord("2", 2000L, 1800, 150, 70f, 3.5f, 0f, 35f)
        )
        val atl = AcuteTrainingLoad.compute(workouts)
        assertThat(atl).isGreaterThan(0f)
    }

    @Test fun `empty workout list gives atl 0`() {
        val atl = AcuteTrainingLoad.compute(emptyList())
        assertThat(atl).isEqualTo(0f)
    }

    @Test fun `readiness label 80 is Primed`() {
        assertThat(ReadinessCalculator.label(80)).isEqualTo("Primed")
    }

    @Test fun `readiness label 0 is Rest`() {
        assertThat(ReadinessCalculator.label(0)).isEqualTo("Rest")
    }
}
