package dev.hossain.keepalive.ui.screen

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Data class representing an application that the user has configured for monitoring.
 *
 * @property packageName The unique package identifier of the application (e.g., "com.example.app").
 * @property appName The human-readable name of the application as displayed to the user.
 * @property isSticky Whether this app should be relaunched at the end of each check cycle
 *   to bring it to the top of the recent apps list. Only one app can be sticky at a time.
 */
@Serializable
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSticky: Boolean = false,
)

/**
 * [Serializer] implementation for persisting a [List] of [AppInfo] objects in a DataStore.
 *
 * Serializes the list to a JSON string when writing and deserializes it when reading.
 * Returns an empty list as the default value if no data has been stored yet or
 * if deserialization fails.
 */
object AppListSerializer : Serializer<List<AppInfo>> {
    /** Default value returned by DataStore when no data has been written yet. */
    override val defaultValue: List<AppInfo> = emptyList()

    /**
     * Deserializes a [List] of [AppInfo] from [input] by reading all bytes and parsing the
     * resulting JSON string. Returns an empty list if the stream is empty or deserialization fails.
     */
    override suspend fun readFrom(input: InputStream): List<AppInfo> {
        return try {
            val jsonString = input.readBytes().decodeToString()
            Json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Serializes [t] (a [List] of [AppInfo]) to JSON and writes the encoded bytes to [output].
     */
    override suspend fun writeTo(
        t: List<AppInfo>,
        output: OutputStream,
    ) {
        val jsonString = Json.encodeToString(t)
        output.write(jsonString.encodeToByteArray())
    }
}
