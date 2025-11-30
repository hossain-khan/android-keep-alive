package dev.hossain.keepalive.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LogActionType] enum.
 */
class LogActionTypeTest {
    @Test
    fun `displayName returns correct names for all types`() {
        assertEquals("All", LogActionType.ALL.displayName())
        assertEquals("Started", LogActionType.STARTED.displayName())
        assertEquals("Already Running", LogActionType.ALREADY_RUNNING.displayName())
        assertEquals("Failed/Unknown", LogActionType.FAILED.displayName())
    }

    @Test
    fun `fromLog returns STARTED when wasAttemptedToStart is true`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Test App",
                wasRunningRecently = false,
                wasAttemptedToStart = true,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals(LogActionType.STARTED, LogActionType.fromLog(log))
    }

    @Test
    fun `fromLog returns STARTED when wasAttemptedToStart is true and forceStartEnabled`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Test App",
                wasRunningRecently = false,
                wasAttemptedToStart = true,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = true,
            )

        assertEquals(LogActionType.STARTED, LogActionType.fromLog(log))
    }

    @Test
    fun `fromLog returns ALREADY_RUNNING when wasRunningRecently is true`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Test App",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals(LogActionType.ALREADY_RUNNING, LogActionType.fromLog(log))
    }

    @Test
    fun `fromLog returns FAILED when neither started nor running`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Test App",
                wasRunningRecently = false,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals(LogActionType.FAILED, LogActionType.fromLog(log))
    }

    @Test
    fun `fromLog prioritizes STARTED over ALREADY_RUNNING`() {
        // If both wasAttemptedToStart and wasRunningRecently are true,
        // STARTED should take priority
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Test App",
                wasRunningRecently = true,
                wasAttemptedToStart = true,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals(LogActionType.STARTED, LogActionType.fromLog(log))
    }

    @Test
    fun `LogActionType enum has four values`() {
        val values = LogActionType.entries.toTypedArray()
        assertEquals(4, values.size)
    }

    @Test
    fun `all displayNames are non-empty`() {
        LogActionType.entries.forEach { type ->
            assert(type.displayName().isNotEmpty()) {
                "Display name for ${type.name} should not be empty"
            }
        }
    }
}
