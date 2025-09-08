package dev.hossain.keepalive.ui.theme

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit test for ThemeMode enum
 */
class ThemeModeTest {
    
    @Test
    fun themeModeValues_shouldHaveThreeOptions() {
        val values = ThemeMode.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(ThemeMode.LIGHT))
        assertTrue(values.contains(ThemeMode.DARK))
        assertTrue(values.contains(ThemeMode.SYSTEM))
    }
    
    @Test
    fun themeModeValueOf_shouldReturnCorrectEnum() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
    }
    
    @Test
    fun themeModeToString_shouldReturnCorrectString() {
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
    }
}