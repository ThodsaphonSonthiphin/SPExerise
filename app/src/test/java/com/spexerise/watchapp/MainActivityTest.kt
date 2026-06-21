package com.spexerise.watchapp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * TDD: Failing test written before implementation.
 * Verifies the greeting text that MainActivity displays.
 *
 * This test will pass once MainActivity renders "Hello Watch".
 * For UI verification on device/emulator, see the androidTest suite.
 */
class MainActivityTest {

    @Test
    fun `greeting text is Hello Watch`() {
        val expectedGreeting = "Hello Watch"
        // The greeting constant exposed for testability
        assertThat(MainActivityGreeting.TEXT).isEqualTo(expectedGreeting)
    }
}
