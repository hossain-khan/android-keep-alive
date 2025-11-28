package dev.hossain.keepalive.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AirtableConfig] data class.
 */
class AirtableConfigTest {
    @Test
    fun `isValid returns true when enabled, token present, and URL is valid`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "valid_token",
                dataUrl = "https://api.airtable.com/v0/base/table",
            )

        assertTrue(config.isValid())
    }

    @Test
    fun `isValid returns false when isEnabled is false`() {
        val config =
            AirtableConfig(
                isEnabled = false,
                token = "valid_token",
                dataUrl = "https://api.airtable.com/v0/base/table",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `isValid returns false when token is empty`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "",
                dataUrl = "https://api.airtable.com/v0/base/table",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `isValid returns false when token is blank`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "   ",
                dataUrl = "https://api.airtable.com/v0/base/table",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `isValid returns false when dataUrl is invalid`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "valid_token",
                dataUrl = "not a valid url",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `isValid returns false when dataUrl is empty`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "valid_token",
                dataUrl = "",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `isValid returns false when multiple conditions are invalid`() {
        val config =
            AirtableConfig(
                isEnabled = false,
                token = "",
                dataUrl = "invalid",
            )

        assertFalse(config.isValid())
    }

    @Test
    fun `data class properties are correctly stored`() {
        val config =
            AirtableConfig(
                isEnabled = true,
                token = "test_token",
                dataUrl = "https://example.com",
            )

        assertTrue(config.isEnabled)
        assertEquals("test_token", config.token)
        assertEquals("https://example.com", config.dataUrl)
    }
}
