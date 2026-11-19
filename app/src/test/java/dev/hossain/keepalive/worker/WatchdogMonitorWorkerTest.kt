package dev.hossain.keepalive.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WatchdogMonitorWorkerTest {
    @Test
    fun `does not start service when no apps are configured`() {
        val decision =
            WatchdogMonitorWorker.evaluateRestartDecision(
                isServiceRunning = false,
                isMonitoringPaused = false,
                hasConfiguredApps = false,
                lastCheckTimeMs = 0L,
                checkIntervalMin = 5,
                nowMs = 1_000_000L,
            )

        assertFalse(decision.shouldStartOrRestart)
    }

    @Test
    fun `does not start service when monitoring is paused`() {
        val decision =
            WatchdogMonitorWorker.evaluateRestartDecision(
                isServiceRunning = false,
                isMonitoringPaused = true,
                hasConfiguredApps = true,
                lastCheckTimeMs = 0L,
                checkIntervalMin = 5,
                nowMs = 1_000_000L,
            )

        assertFalse(decision.shouldStartOrRestart)
    }

    @Test
    fun `starts service when not running and monitoring is active`() {
        val decision =
            WatchdogMonitorWorker.evaluateRestartDecision(
                isServiceRunning = false,
                isMonitoringPaused = false,
                hasConfiguredApps = true,
                lastCheckTimeMs = 0L,
                checkIntervalMin = 5,
                nowMs = 1_000_000L,
            )

        assertTrue(decision.shouldStartOrRestart)
        assertFalse(decision.forceRestart)
    }

    @Test
    fun `does not restart service when running and last check is recent`() {
        val now = 10_000_000L
        val recentCheck = now - TimeUnit.MINUTES.toMillis(10)

        val decision =
            WatchdogMonitorWorker.evaluateRestartDecision(
                isServiceRunning = true,
                isMonitoringPaused = false,
                hasConfiguredApps = true,
                lastCheckTimeMs = recentCheck,
                checkIntervalMin = 5,
                nowMs = now,
            )

        assertFalse(decision.shouldStartOrRestart)
    }

    @Test
    fun `restarts service when running but last check is stale`() {
        val now = 10_000_000L
        val staleCheck = now - TimeUnit.MINUTES.toMillis(35)

        val decision =
            WatchdogMonitorWorker.evaluateRestartDecision(
                isServiceRunning = true,
                isMonitoringPaused = false,
                hasConfiguredApps = true,
                lastCheckTimeMs = staleCheck,
                checkIntervalMin = 5,
                nowMs = now,
            )

        assertTrue(decision.shouldStartOrRestart)
        assertTrue(decision.forceRestart)
    }
}
