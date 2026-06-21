package com.spexerise.watchapp.data.db
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntityTest {
    @Test fun `WorkoutRecord copy produces distinct object with changed field`() {
        val original = WorkoutRecord("1", 1000L, 1800, 145, 55f, 3.1f, 0.5f, 42f)
        val copy = original.copy(id = "2")
        assertThat(copy.id).isEqualTo("2")
        assertThat(copy.avgHrBpm).isEqualTo(original.avgHrBpm)
    }

    @Test fun `ReadinessSnapshot equality holds for same fields`() {
        val a = ReadinessSnapshot(1L, 75, 45f, 7.5f, 0.9f, 30f, 58)
        val b = ReadinessSnapshot(1L, 75, 45f, 7.5f, 0.9f, 30f, 58)
        assertThat(a).isEqualTo(b)
    }

    @Test fun `Vo2MaxRecord source field stores string value`() {
        val r = Vo2MaxRecord(System.currentTimeMillis(), 48.5f, "samsung_health")
        assertThat(r.source).isEqualTo("samsung_health")
    }
}
