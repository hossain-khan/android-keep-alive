package dev.hossain.keepalive.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppConfig] configuration constants.
 */
class AppConfigTest {
    @Test
    fun `MINIMUM_APP_CHECK_INTERVAL_MIN has expected value`() {
        assertEquals(5, AppConfig.MINIMUM_APP_CHECK_INTERVAL_MIN)
    }

    @Test
    fun `DEFAULT_APP_CHECK_INTERVAL_MIN has expected value`() {
        assertEquals(30, AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN)
    }

    @Test
    fun `DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS has expected value`() {
        assertEquals(10_000L, AppConfig.DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS)
    }

    @Test
    fun `MIN_APP_CHECK_INTERVAL_SLIDER has expected value`() {
        assertEquals(10, AppConfig.MIN_APP_CHECK_INTERVAL_SLIDER)
    }

    @Test
    fun `MAX_APP_CHECK_INTERVAL_SLIDER has expected value of 24 hours`() {
        assertEquals(1440, AppConfig.MAX_APP_CHECK_INTERVAL_SLIDER)
    }

    @Test
    fun `APP_CHECK_INTERVAL_STEP has expected value`() {
        assertEquals(5, AppConfig.APP_CHECK_INTERVAL_STEP)
    }

    @Test
    fun `DEFAULT_APP_CHECK_INTERVAL_MIN is greater than or equal to MINIMUM_APP_CHECK_INTERVAL_MIN`() {
        assertTrue(
            "Default interval should be >= minimum interval",
            AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN >= AppConfig.MINIMUM_APP_CHECK_INTERVAL_MIN,
        )
    }

    @Test
    fun `slider range is valid with min less than max`() {
        assertTrue(
            "Slider min should be less than max",
            AppConfig.MIN_APP_CHECK_INTERVAL_SLIDER < AppConfig.MAX_APP_CHECK_INTERVAL_SLIDER,
        )
    }

    @Test
    fun `step size evenly divides slider range`() {
        val range = AppConfig.MAX_APP_CHECK_INTERVAL_SLIDER - AppConfig.MIN_APP_CHECK_INTERVAL_SLIDER
        assertTrue(
            "Step size should evenly divide the slider range",
            range % AppConfig.APP_CHECK_INTERVAL_STEP == 0,
        )
    }
}
