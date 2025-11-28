package dev.hossain.keepalive.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Screen] enum representing app navigation destinations.
 */
class ScreenTest {
    @Test
    fun `Screen enum has four screens`() {
        val values = Screen.entries.toTypedArray()
        assertEquals(4, values.size)
    }

    @Test
    fun `Home screen has correct route`() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun `Home screen has correct title`() {
        assertEquals("Home", Screen.Home.title)
    }

    @Test
    fun `Home screen has correct icon`() {
        assertEquals(Icons.Filled.Home, Screen.Home.icon)
    }

    @Test
    fun `WatchDogConfig screen has correct route`() {
        assertEquals("app_configs", Screen.WatchDogConfig.route)
    }

    @Test
    fun `WatchDogConfig screen has correct title`() {
        assertEquals("Config", Screen.WatchDogConfig.title)
    }

    @Test
    fun `WatchDogConfig screen has correct icon`() {
        assertEquals(Icons.Filled.Settings, Screen.WatchDogConfig.icon)
    }

    @Test
    fun `AppWatchList screen has correct route`() {
        assertEquals("app_watch_list", Screen.AppWatchList.route)
    }

    @Test
    fun `AppWatchList screen has correct title`() {
        assertEquals("Apps", Screen.AppWatchList.title)
    }

    @Test
    fun `AppWatchList screen has correct icon`() {
        assertEquals(Icons.AutoMirrored.Filled.List, Screen.AppWatchList.icon)
    }

    @Test
    fun `ActivityLogs screen has correct route`() {
        assertEquals("activity_logs", Screen.ActivityLogs.route)
    }

    @Test
    fun `ActivityLogs screen has correct title`() {
        assertEquals("Logs", Screen.ActivityLogs.title)
    }

    @Test
    fun `ActivityLogs screen has correct icon`() {
        assertEquals(Icons.Filled.Info, Screen.ActivityLogs.icon)
    }

    @Test
    fun `all screen routes are unique`() {
        val routes = Screen.entries.map { it.route }
        assertEquals("All routes should be unique", routes.size, routes.toSet().size)
    }

    @Test
    fun `all screen routes are non-empty`() {
        Screen.entries.forEach { screen ->
            assertTrue(
                "Route for ${screen.name} should not be empty",
                screen.route.isNotEmpty(),
            )
        }
    }

    @Test
    fun `all screen titles are non-empty`() {
        Screen.entries.forEach { screen ->
            assertTrue(
                "Title for ${screen.name} should not be empty",
                screen.title.isNotEmpty(),
            )
        }
    }

    @Test
    fun `valueOf returns correct Screen for each name`() {
        assertEquals(Screen.Home, Screen.valueOf("Home"))
        assertEquals(Screen.WatchDogConfig, Screen.valueOf("WatchDogConfig"))
        assertEquals(Screen.AppWatchList, Screen.valueOf("AppWatchList"))
        assertEquals(Screen.ActivityLogs, Screen.valueOf("ActivityLogs"))
    }

    @Test
    fun `ordinal values are sequential`() {
        val values = Screen.entries.toTypedArray()
        values.forEachIndexed { index, screen ->
            assertEquals(
                "Ordinal for ${screen.name} should be $index",
                index,
                screen.ordinal,
            )
        }
    }
}
