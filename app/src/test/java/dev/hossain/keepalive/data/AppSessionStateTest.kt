package dev.hossain.keepalive.data

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppSessionState] in-memory singleton state holder.
 */
class AppSessionStateTest {
    @After
    fun tearDown() {
        // Reset state after each test to avoid cross-test pollution
        AppSessionState.isBatteryWarningDismissed = false
    }

    @Test
    fun `isBatteryWarningDismissed defaults to false`() {
        assertFalse(AppSessionState.isBatteryWarningDismissed)
    }

    @Test
    fun `isBatteryWarningDismissed can be set to true`() {
        AppSessionState.isBatteryWarningDismissed = true
        assertTrue(AppSessionState.isBatteryWarningDismissed)
    }

    @Test
    fun `isBatteryWarningDismissed can be reset to false`() {
        AppSessionState.isBatteryWarningDismissed = true
        AppSessionState.isBatteryWarningDismissed = false
        assertFalse(AppSessionState.isBatteryWarningDismissed)
    }

    @Test
    fun `isBatteryWarningDismissed reflects most recent assignment`() {
        AppSessionState.isBatteryWarningDismissed = true
        assertTrue(AppSessionState.isBatteryWarningDismissed)
        AppSessionState.isBatteryWarningDismissed = false
        assertFalse(AppSessionState.isBatteryWarningDismissed)
        AppSessionState.isBatteryWarningDismissed = true
        assertTrue(AppSessionState.isBatteryWarningDismissed)
    }
}
