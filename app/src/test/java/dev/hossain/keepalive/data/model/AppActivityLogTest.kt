package dev.hossain.keepalive.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AppActivityLog] data class.
 */
class AppActivityLogTest {
    @Test
    fun `getStatusSummary returns Force started when wasAttemptedToStart and forceStartEnabled`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = false,
                wasAttemptedToStart = true,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = true,
            )

        assertEquals("Force started", log.getStatusSummary())
    }

    @Test
    fun `getStatusSummary returns Started when wasAttemptedToStart and forceStartEnabled is false`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = false,
                wasAttemptedToStart = true,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals("Started (was not running)", log.getStatusSummary())
    }

    @Test
    fun `getStatusSummary returns Running normally when wasRunningRecently is true`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals("Running normally", log.getStatusSummary())
    }

    @Test
    fun `getStatusSummary returns Unknown state when no conditions met`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = false,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals("Unknown state", log.getStatusSummary())
    }

    @Test
    fun `getFormattedTimestamp returns properly formatted date`() {
        // Using a known timestamp: 1699564800000 is Nov 10, 2023 12:00 AM UTC
        // Note: The exact format will depend on the default Locale
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = 1699564800000L,
                forceStartEnabled = false,
            )

        // The format should be "MMM d, yyyy h:mm a" like "Nov 10, 2023 12:00 AM"
        val formattedTimestamp = log.getFormattedTimestamp()

        // Just verify it's not empty and contains some expected parts
        assert(formattedTimestamp.isNotEmpty()) { "Formatted timestamp should not be empty" }
        assert(formattedTimestamp.contains("2023")) { "Formatted timestamp should contain year 2023" }
    }

    @Test
    fun `default message is empty string`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
            )

        assertEquals("", log.message)
    }

    @Test
    fun `message can be set to non-empty string`() {
        val log =
            AppActivityLog(
                packageId = "com.example.app",
                appName = "Example App",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = System.currentTimeMillis(),
                forceStartEnabled = false,
                message = "Custom message",
            )

        assertEquals("Custom message", log.message)
    }

    @Test
    fun `data class properties are correctly stored`() {
        val timestamp = 1699564800000L
        val log =
            AppActivityLog(
                packageId = "com.test.package",
                appName = "Test Application",
                wasRunningRecently = true,
                wasAttemptedToStart = false,
                timestamp = timestamp,
                forceStartEnabled = true,
                message = "Test message",
            )

        assertEquals("com.test.package", log.packageId)
        assertEquals("Test Application", log.appName)
        assertEquals(true, log.wasRunningRecently)
        assertEquals(false, log.wasAttemptedToStart)
        assertEquals(timestamp, log.timestamp)
        assertEquals(true, log.forceStartEnabled)
        assertEquals("Test message", log.message)
    }
}
