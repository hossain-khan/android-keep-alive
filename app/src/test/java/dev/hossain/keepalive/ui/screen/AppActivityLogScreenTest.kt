package dev.hossain.keepalive.ui.screen

import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.data.model.LogActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [DateFilter] enum used in the Activity Log screen.
 */
class DateFilterTest {
    @Test
    fun `displayName returns correct names for all filters`() {
        assertEquals("All Time", DateFilter.ALL.displayName())
        assertEquals("Today", DateFilter.TODAY.displayName())
        assertEquals("Last 7 Days", DateFilter.LAST_7_DAYS.displayName())
        assertEquals("Last 30 Days", DateFilter.LAST_30_DAYS.displayName())
    }

    @Test
    fun `getStartTimestamp returns null for ALL filter`() {
        assertNull(DateFilter.ALL.getStartTimestamp())
    }

    @Test
    fun `getStartTimestamp returns today start for TODAY filter`() {
        val timestamp = DateFilter.TODAY.getStartTimestamp()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // The timestamp should be approximately equal to today's start
        // Allow some margin for test execution time
        assertTrue(timestamp != null)
        assertTrue(kotlin.math.abs(timestamp!! - calendar.timeInMillis) < 1000)
    }

    @Test
    fun `getStartTimestamp returns 7 days ago for LAST_7_DAYS filter`() {
        val timestamp = DateFilter.LAST_7_DAYS.getStartTimestamp()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)

        assertTrue(timestamp != null)
        // Allow 1 second margin for test execution time
        assertTrue(kotlin.math.abs(timestamp!! - calendar.timeInMillis) < 1000)
    }

    @Test
    fun `getStartTimestamp returns 30 days ago for LAST_30_DAYS filter`() {
        val timestamp = DateFilter.LAST_30_DAYS.getStartTimestamp()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30)

        assertTrue(timestamp != null)
        // Allow 1 second margin for test execution time
        assertTrue(kotlin.math.abs(timestamp!! - calendar.timeInMillis) < 1000)
    }

    @Test
    fun `DateFilter enum has four values`() {
        val values = DateFilter.entries.toTypedArray()
        assertEquals(4, values.size)
    }
}

/**
 * Unit tests for filtering functionality in Activity Log screen.
 */
class LogFilteringTest {
    private fun createTestLog(
        appName: String,
        packageId: String,
        wasRunningRecently: Boolean,
        wasAttemptedToStart: Boolean,
        timestamp: Long,
    ): AppActivityLog =
        AppActivityLog(
            packageId = packageId,
            appName = appName,
            wasRunningRecently = wasRunningRecently,
            wasAttemptedToStart = wasAttemptedToStart,
            timestamp = timestamp,
            forceStartEnabled = false,
        )

    @Test
    fun `filterLogs returns all logs when no filters applied`() {
        val logs =
            listOf(
                createTestLog("App1", "com.app1", true, false, System.currentTimeMillis()),
                createTestLog("App2", "com.app2", false, true, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "", LogActionType.ALL, DateFilter.ALL)
        assertEquals(2, filtered.size)
    }

    @Test
    fun `filterLogs filters by app name search query`() {
        val logs =
            listOf(
                createTestLog("Test App", "com.test", true, false, System.currentTimeMillis()),
                createTestLog("Other App", "com.other", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "Test", LogActionType.ALL, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertEquals("Test App", filtered[0].appName)
    }

    @Test
    fun `filterLogs filters by package ID search query`() {
        val logs =
            listOf(
                createTestLog("App1", "com.example.test", true, false, System.currentTimeMillis()),
                createTestLog("App2", "com.other.app", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "example", LogActionType.ALL, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertEquals("com.example.test", filtered[0].packageId)
    }

    @Test
    fun `filterLogs search is case insensitive`() {
        val logs =
            listOf(
                createTestLog("MyApp", "com.myapp", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "MYAPP", LogActionType.ALL, DateFilter.ALL)
        assertEquals(1, filtered.size)
    }

    @Test
    fun `filterLogs filters by STARTED action type`() {
        val logs =
            listOf(
                createTestLog("App1", "com.app1", false, true, System.currentTimeMillis()),
                createTestLog("App2", "com.app2", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "", LogActionType.STARTED, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].wasAttemptedToStart)
    }

    @Test
    fun `filterLogs filters by ALREADY_RUNNING action type`() {
        val logs =
            listOf(
                createTestLog("App1", "com.app1", false, true, System.currentTimeMillis()),
                createTestLog("App2", "com.app2", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "", LogActionType.ALREADY_RUNNING, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertTrue(filtered[0].wasRunningRecently)
    }

    @Test
    fun `filterLogs filters by FAILED action type`() {
        val logs =
            listOf(
                createTestLog("App1", "com.app1", false, true, System.currentTimeMillis()),
                createTestLog("App2", "com.app2", false, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "", LogActionType.FAILED, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertTrue(!filtered[0].wasRunningRecently && !filtered[0].wasAttemptedToStart)
    }

    @Test
    fun `filterLogs filters by date TODAY`() {
        val now = System.currentTimeMillis()
        val yesterdayTimestamp = now - (24 * 60 * 60 * 1000) // 24 hours ago

        val logs =
            listOf(
                createTestLog("App1", "com.app1", true, false, now),
                createTestLog("App2", "com.app2", true, false, yesterdayTimestamp),
            )

        val filtered = filterLogs(logs, "", LogActionType.ALL, DateFilter.TODAY)
        assertEquals(1, filtered.size)
        assertEquals("App1", filtered[0].appName)
    }

    @Test
    fun `filterLogs filters by date LAST_7_DAYS`() {
        val now = System.currentTimeMillis()
        val oneWeekAgoTimestamp = now - (8L * 24 * 60 * 60 * 1000) // 8 days ago

        val logs =
            listOf(
                createTestLog("App1", "com.app1", true, false, now),
                createTestLog("App2", "com.app2", true, false, oneWeekAgoTimestamp),
            )

        val filtered = filterLogs(logs, "", LogActionType.ALL, DateFilter.LAST_7_DAYS)
        assertEquals(1, filtered.size)
        assertEquals("App1", filtered[0].appName)
    }

    @Test
    fun `filterLogs combines multiple filters`() {
        val now = System.currentTimeMillis()

        val logs =
            listOf(
                createTestLog("Test App", "com.test", false, true, now),
                createTestLog("Test Other", "com.other", true, false, now),
                createTestLog("Another App", "com.another", false, true, now),
            )

        val filtered = filterLogs(logs, "Test", LogActionType.STARTED, DateFilter.ALL)
        assertEquals(1, filtered.size)
        assertEquals("Test App", filtered[0].appName)
    }

    @Test
    fun `filterLogs returns empty list when no logs match`() {
        val logs =
            listOf(
                createTestLog("App1", "com.app1", true, false, System.currentTimeMillis()),
            )

        val filtered = filterLogs(logs, "NonExistent", LogActionType.ALL, DateFilter.ALL)
        assertTrue(filtered.isEmpty())
    }

    /**
     * Helper function to test filtering logic.
     * This duplicates the private function from AppActivityLogScreen for testing purposes.
     */
    private fun filterLogs(
        logs: List<AppActivityLog>,
        searchQuery: String,
        actionType: LogActionType,
        dateFilter: DateFilter,
    ): List<AppActivityLog> {
        var filtered = logs

        // Filter by search query (app name or package ID)
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            filtered =
                filtered.filter { log ->
                    log.appName.lowercase().contains(query) ||
                        log.packageId.lowercase().contains(query)
                }
        }

        // Filter by action type
        if (actionType != LogActionType.ALL) {
            filtered =
                filtered.filter { log ->
                    LogActionType.fromLog(log) == actionType
                }
        }

        // Filter by date
        val startTimestamp = dateFilter.getStartTimestamp()
        if (startTimestamp != null) {
            filtered = filtered.filter { log -> log.timestamp >= startTimestamp }
        }

        return filtered
    }
}
