package dev.hossain.keepalive.data

import dev.hossain.keepalive.util.Validator.isValidUrl

/**
 * Represents the configuration for Airtable integration used for remote logging.
 *
 * @property isEnabled Whether remote logging to Airtable is currently enabled by the user.
 * @property token The Airtable personal access token used to authenticate API requests.
 * @property dataUrl The full Airtable API endpoint URL (e.g., `https://api.airtable.com/v0/appXXXX/RemoteLog`).
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
