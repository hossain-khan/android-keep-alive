package dev.hossain.keepalive.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [Validator] utility object.
 */
class ValidatorTest {
    // ========== URL Validation Tests ==========

    @Test
    fun `isValidUrl returns true for valid https URL`() {
        assertTrue(Validator.isValidUrl("https://example.com"))
    }

    @Test
    fun `isValidUrl returns true for valid http URL`() {
        assertTrue(Validator.isValidUrl("http://example.com"))
    }

    @Test
    fun `isValidUrl returns true for valid ftp URL`() {
        assertTrue(Validator.isValidUrl("ftp://example.com"))
    }

    @Test
    fun `isValidUrl returns true for URL with path`() {
        assertTrue(Validator.isValidUrl("https://example.com/path/to/resource"))
    }

    @Test
    fun `isValidUrl returns true for URL with query parameters`() {
        assertTrue(Validator.isValidUrl("https://example.com?param=value"))
    }

    @Test
    fun `isValidUrl returns true for URL with port`() {
        assertTrue(Validator.isValidUrl("https://example.com:8080"))
    }

    @Test
    fun `isValidUrl returns true for URL with subdomain`() {
        assertTrue(Validator.isValidUrl("https://api.example.com"))
    }

    @Test
    fun `isValidUrl returns false for empty string`() {
        assertFalse(Validator.isValidUrl(""))
    }

    @Test
    fun `isValidUrl returns false for plain text`() {
        assertFalse(Validator.isValidUrl("not a url"))
    }

    @Test
    fun `isValidUrl returns false for URL without scheme`() {
        assertFalse(Validator.isValidUrl("example.com"))
    }

    @Test
    fun `isValidUrl returns false for URL with invalid scheme`() {
        assertFalse(Validator.isValidUrl("mailto:test@example.com"))
    }

    @Test
    fun `isValidUrl returns false for URL with only scheme`() {
        assertFalse(Validator.isValidUrl("https://"))
    }

    // ========== UUID Validation Tests ==========

    @Test
    fun `isValidUUID returns true for valid UUID lowercase`() {
        assertTrue(Validator.isValidUUID("123e4567-e89b-12d3-a456-426614174000"))
    }

    @Test
    fun `isValidUUID returns true for valid UUID uppercase`() {
        assertTrue(Validator.isValidUUID("123E4567-E89B-12D3-A456-426614174000"))
    }

    @Test
    fun `isValidUUID returns true for valid UUID mixed case`() {
        assertTrue(Validator.isValidUUID("123E4567-e89b-12D3-a456-426614174000"))
    }

    @Test
    fun `isValidUUID returns true for all zeros UUID`() {
        assertTrue(Validator.isValidUUID("00000000-0000-0000-0000-000000000000"))
    }

    @Test
    fun `isValidUUID returns true for all f UUID`() {
        assertTrue(Validator.isValidUUID("ffffffff-ffff-ffff-ffff-ffffffffffff"))
    }

    @Test
    fun `isValidUUID returns false for empty string`() {
        assertFalse(Validator.isValidUUID(""))
    }

    @Test
    fun `isValidUUID returns false for plain text`() {
        assertFalse(Validator.isValidUUID("not a uuid"))
    }

    @Test
    fun `isValidUUID returns false for UUID without hyphens`() {
        assertFalse(Validator.isValidUUID("123e4567e89b12d3a456426614174000"))
    }

    @Test
    fun `isValidUUID returns false for UUID with wrong segment lengths`() {
        assertFalse(Validator.isValidUUID("123e456-e89b-12d3-a456-426614174000"))
    }

    @Test
    fun `isValidUUID returns false for UUID with invalid characters`() {
        assertFalse(Validator.isValidUUID("123g4567-e89b-12d3-a456-426614174000"))
    }

    @Test
    fun `isValidUUID returns false for UUID with extra characters`() {
        assertFalse(Validator.isValidUUID("123e4567-e89b-12d3-a456-4266141740001"))
    }

    @Test
    fun `isValidUUID returns false for UUID with missing characters`() {
        assertFalse(Validator.isValidUUID("123e4567-e89b-12d3-a456-42661417400"))
    }
}
