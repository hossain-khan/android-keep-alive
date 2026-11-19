package dev.hossain.keepalive.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionHealthCheckerTest {
    @Test
    fun `when all critical permissions are granted, does not alert and resets alertShown`() {
        val decision =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = true,
                wasPreviouslyGranted = false,
                alertAlreadyShown = true,
                isAppConfigured = false,
            )

        assertFalse(decision.shouldAlert)
        assertTrue(decision.nextWasGranted)
        assertFalse(decision.nextAlertShown)
    }

    @Test
    fun `when permissions missing on fresh unconfigured install, does not alert`() {
        val decision =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = false,
                wasPreviouslyGranted = false,
                alertAlreadyShown = false,
                isAppConfigured = false,
            )

        assertFalse(decision.shouldAlert)
        assertFalse(decision.nextWasGranted)
        assertFalse(decision.nextAlertShown)
    }

    @Test
    fun `when permissions revoked after previously granted, alerts once`() {
        val first =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = false,
                wasPreviouslyGranted = true,
                alertAlreadyShown = false,
                isAppConfigured = true,
            )

        assertTrue(first.shouldAlert)
        assertTrue(first.nextWasGranted)
        assertTrue(first.nextAlertShown)

        val second =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = false,
                wasPreviouslyGranted = first.nextWasGranted,
                alertAlreadyShown = first.nextAlertShown,
                isAppConfigured = true,
            )

        assertFalse(second.shouldAlert)
        assertTrue(second.nextAlertShown)
    }

    @Test
    fun `when permissions missing for configured app on upgrade, alerts once`() {
        val decision =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = false,
                wasPreviouslyGranted = false,
                alertAlreadyShown = false,
                isAppConfigured = true,
            )

        assertEquals(
            PermissionHealthChecker.PermissionAlertDecision(
                shouldAlert = true,
                nextWasGranted = false,
                nextAlertShown = true,
            ),
            decision,
        )
    }

    @Test
    fun `when permissions restored and later revoked again, alerts again`() {
        val restored =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = true,
                wasPreviouslyGranted = true,
                alertAlreadyShown = true,
                isAppConfigured = true,
            )
        assertFalse(restored.nextAlertShown)

        val revokedAgain =
            PermissionHealthChecker.evaluatePermissionAlertTransition(
                allCriticalGranted = false,
                wasPreviouslyGranted = restored.nextWasGranted,
                alertAlreadyShown = restored.nextAlertShown,
                isAppConfigured = true,
            )
        assertTrue(revokedAgain.shouldAlert)
        assertTrue(revokedAgain.nextAlertShown)
    }
}
