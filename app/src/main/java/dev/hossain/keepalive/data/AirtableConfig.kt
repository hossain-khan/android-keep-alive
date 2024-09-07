package dev.hossain.keepalive.data

data class AirtableConfig(
    val isEnabled: Boolean,
    val token: String,
    val dataUrl: String,
) {
    // Check if the Airtable configuration is valid
    fun isValid(): Boolean {
        return isEnabled && token.isNotBlank() && isValidUrl(dataUrl)
    }

    private fun isValidUrl(url: String): Boolean {
        val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
        return url.matches(urlRegex)
    }
}
