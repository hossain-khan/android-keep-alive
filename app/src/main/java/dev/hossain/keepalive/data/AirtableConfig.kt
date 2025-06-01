package dev.hossain.keepalive.data

import dev.hossain.keepalive.util.Validator.isValidUrl

/**
 * Represents the configuration for Airtable integration.
 */
data class AirtableConfig(
    val isEnabled: Boolean,
    val token: String,
    val dataUrl: String,
) {
    /**
     * Checks if the Airtable configuration is valid, ensuring that the feature is enabled,
     * a token is present, and the data URL is a valid URL format.
     */
    fun isValid(): Boolean {
        return isEnabled && token.isNotBlank() && isValidUrl(dataUrl)
    }
}
