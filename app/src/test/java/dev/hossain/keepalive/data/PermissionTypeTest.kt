package dev.hossain.keepalive.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PermissionType] enum.
 */
class PermissionTypeTest {
    @Test
    fun `PermissionType has four permission values`() {
        val values = PermissionType.entries.toTypedArray()
        assertEquals(4, values.size)
    }

    @Test
    fun `PermissionType contains PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS`() {
        val values = PermissionType.entries.toTypedArray()
        assertTrue(values.contains(PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS))
    }

    @Test
    fun `PermissionType contains PERMISSION_POST_NOTIFICATIONS`() {
        val values = PermissionType.entries.toTypedArray()
        assertTrue(values.contains(PermissionType.PERMISSION_POST_NOTIFICATIONS))
    }

    @Test
    fun `PermissionType contains PERMISSION_PACKAGE_USAGE_STATS`() {
        val values = PermissionType.entries.toTypedArray()
        assertTrue(values.contains(PermissionType.PERMISSION_PACKAGE_USAGE_STATS))
    }

    @Test
    fun `PermissionType contains PERMISSION_SYSTEM_APPLICATION_OVERLAY`() {
        val values = PermissionType.entries.toTypedArray()
        assertTrue(values.contains(PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY))
    }

    @Test
    fun `valueOf returns correct enum for PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS`() {
        assertEquals(
            PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS,
            PermissionType.valueOf("PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS"),
        )
    }

    @Test
    fun `valueOf returns correct enum for PERMISSION_POST_NOTIFICATIONS`() {
        assertEquals(
            PermissionType.PERMISSION_POST_NOTIFICATIONS,
            PermissionType.valueOf("PERMISSION_POST_NOTIFICATIONS"),
        )
    }

    @Test
    fun `valueOf returns correct enum for PERMISSION_PACKAGE_USAGE_STATS`() {
        assertEquals(
            PermissionType.PERMISSION_PACKAGE_USAGE_STATS,
            PermissionType.valueOf("PERMISSION_PACKAGE_USAGE_STATS"),
        )
    }

    @Test
    fun `valueOf returns correct enum for PERMISSION_SYSTEM_APPLICATION_OVERLAY`() {
        assertEquals(
            PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY,
            PermissionType.valueOf("PERMISSION_SYSTEM_APPLICATION_OVERLAY"),
        )
    }

    @Test
    fun `name property returns correct string for each permission`() {
        assertEquals(
            "PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS",
            PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS.name,
        )
        assertEquals(
            "PERMISSION_POST_NOTIFICATIONS",
            PermissionType.PERMISSION_POST_NOTIFICATIONS.name,
        )
        assertEquals(
            "PERMISSION_PACKAGE_USAGE_STATS",
            PermissionType.PERMISSION_PACKAGE_USAGE_STATS.name,
        )
        assertEquals(
            "PERMISSION_SYSTEM_APPLICATION_OVERLAY",
            PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY.name,
        )
    }

    @Test
    fun `ordinal values are sequential starting from zero`() {
        val values = PermissionType.entries.toTypedArray()
        values.forEachIndexed { index, permissionType ->
            assertEquals(
                "Ordinal for ${permissionType.name} should be $index",
                index,
                permissionType.ordinal,
            )
        }
    }
}
