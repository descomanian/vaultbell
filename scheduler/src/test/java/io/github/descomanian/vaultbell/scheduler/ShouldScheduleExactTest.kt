package io.github.descomanian.vaultbell.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression test for the operator precedence bug in v1.0.0 (NoSuchMethodError on Android 8–11). */
class ShouldScheduleExactTest {
    private var calls = 0

    private fun permission(granted: Boolean): () -> Boolean = {
        calls++
        granted
    }

    @Test
    fun inexactRequestNeverAsksForPermission() {
        for (sdk in listOf(26, 30, 31, 35)) {
            for (granted in listOf(true, false)) {
                assertFalse(shouldScheduleExact(exact = false, sdkInt = sdk, canScheduleExactAlarms = permission(granted)))
            }
        }
        assertEquals("canScheduleExactAlarms must not be called for inexact alarms", 0, calls)
    }

    @Test
    fun belowAndroid12ExactWithoutPermissionCall() {
        for (sdk in 26..30) {
            assertTrue(shouldScheduleExact(exact = true, sdkInt = sdk, canScheduleExactAlarms = permission(false)))
        }
        assertEquals("API 31 method must not be called below Android 12", 0, calls)
    }

    @Test
    fun fromAndroid12FollowsPermission() {
        for (sdk in 31..35) {
            assertTrue(shouldScheduleExact(exact = true, sdkInt = sdk, canScheduleExactAlarms = permission(true)))
            assertFalse(shouldScheduleExact(exact = true, sdkInt = sdk, canScheduleExactAlarms = permission(false)))
        }
        assertEquals(10, calls)
    }
}
