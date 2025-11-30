package dev.hossain.keepalive.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NotificationVerbosity] enum.
 */
class NotificationVerbosityTest {
    @Test
    fun `NotificationVerbosity has three verbosity levels`() {
        val values = NotificationVerbosity.entries.toTypedArray()
        assertEquals(3, values.size)
    }

    @Test
    fun `NotificationVerbosity contains QUIET`() {
        val values = NotificationVerbosity.entries.toTypedArray()
        assertTrue(values.contains(NotificationVerbosity.QUIET))
    }

    @Test
    fun `NotificationVerbosity contains NORMAL`() {
        val values = NotificationVerbosity.entries.toTypedArray()
        assertTrue(values.contains(NotificationVerbosity.NORMAL))
    }

    @Test
    fun `NotificationVerbosity contains VERBOSE`() {
        val values = NotificationVerbosity.entries.toTypedArray()
        assertTrue(values.contains(NotificationVerbosity.VERBOSE))
    }

    @Test
    fun `valueOf returns correct enum for QUIET`() {
        assertEquals(
            NotificationVerbosity.QUIET,
            NotificationVerbosity.valueOf("QUIET"),
        )
    }

    @Test
    fun `valueOf returns correct enum for NORMAL`() {
        assertEquals(
            NotificationVerbosity.NORMAL,
            NotificationVerbosity.valueOf("NORMAL"),
        )
    }

    @Test
    fun `valueOf returns correct enum for VERBOSE`() {
        assertEquals(
            NotificationVerbosity.VERBOSE,
            NotificationVerbosity.valueOf("VERBOSE"),
        )
    }

    @Test
    fun `name property returns correct string for each verbosity level`() {
        assertEquals("QUIET", NotificationVerbosity.QUIET.name)
        assertEquals("NORMAL", NotificationVerbosity.NORMAL.name)
        assertEquals("VERBOSE", NotificationVerbosity.VERBOSE.name)
    }

    @Test
    fun `ordinal values are sequential starting from zero`() {
        val values = NotificationVerbosity.entries.toTypedArray()
        values.forEachIndexed { index, verbosity ->
            assertEquals(
                "Ordinal for ${verbosity.name} should be $index",
                index,
                verbosity.ordinal,
            )
        }
    }

    @Test
    fun `ordinal order is QUIET, NORMAL, VERBOSE`() {
        assertEquals(0, NotificationVerbosity.QUIET.ordinal)
        assertEquals(1, NotificationVerbosity.NORMAL.ordinal)
        assertEquals(2, NotificationVerbosity.VERBOSE.ordinal)
    }
}
