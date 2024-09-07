package dev.hossain.keepalive.data

import dev.hossain.keepalive.util.Validator.isValidUrl

data class AirtableConfig(
    val isEnabled: Boolean,
    val token: String,
    val dataUrl: String,
) {
    // Check if the Airtable configuration is valid
    fun isValid(): Boolean {
        return isEnabled && token.isNotBlank() && isValidUrl(dataUrl)
    }
}
