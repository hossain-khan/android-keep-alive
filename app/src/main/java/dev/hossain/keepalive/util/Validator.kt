package dev.hossain.keepalive.util

/**
 * Utility object for performing various kinds of validation.
 *
 * Currently provides methods to validate:
 * - URL strings ([isValidUrl])
 * - UUID strings ([isValidUUID])
 */
object Validator {
    /**
     * Validates if the given string is a well-formed URL.
     *
     * The regex checks for common URL schemes (http, https, ftp) followed by "://",
     * and then a domain name part. It's a basic check and might not cover all edge cases
     * of URL validation but is generally sufficient for typical use cases.
     *
     * @param url The string to validate as a URL.
     * @return `true` if the string matches the URL pattern, `false` otherwise.
     */
    fun isValidUrl(url: String): Boolean {
        val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
        return url.matches(urlRegex)
    }

    /**
     * Validates if the given string is a well-formed UUID (Universally Unique Identifier).
     *
     * The regex checks for the standard UUID format: 8-4-4-4-12 hexadecimal characters,
     * separated by hyphens. It allows for uppercase or lowercase hexadecimal digits.
     *
     * Example of a valid UUID: `123e4567-e89b-12d3-a456-426614174000`
     *
     * @param uuid The string to validate as a UUID.
     * @return `true` if the string matches the UUID pattern, `false` otherwise.
     */
    fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$".toRegex()
        return uuid.matches(uuidRegex)
    }
}
