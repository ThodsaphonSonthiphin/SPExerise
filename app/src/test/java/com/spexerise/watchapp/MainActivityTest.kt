package com.spexerise.watchapp

import androidx.activity.ComponentActivity
import org.junit.Test
import kotlin.reflect.full.superclasses

/**
 * Smoke test verifying MainActivity is a ComponentActivity subclass.
 * The old greeting-based test was removed when navigation replaced
 * the static "Hello Watch" screen in Task 9.
 */
class MainActivityTest {

    @Test
    fun `MainActivity extends ComponentActivity`() {
        val superclasses = MainActivity::class.superclasses
        assert(superclasses.any { it == ComponentActivity::class }) {
            "Expected MainActivity to extend ComponentActivity"
        }
    }
}
