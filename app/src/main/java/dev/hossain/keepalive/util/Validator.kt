package dev.hossain.keepalive.util

object Validator {
    fun isValidUrl(url: String): Boolean {
        val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
        return url.matches(urlRegex)
    }

    fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$".toRegex()
        return uuid.matches(uuidRegex)
    }
}
