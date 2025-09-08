package dev.hossain.keepalive.ui.screen

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppInfo(
    val packageName: String,
    val appName: String,
    /**
     * Flag to indicate if this app should be started at the end
     * to bring it on top of the recent apps list.
     */
    val isSticky: Boolean = false,
)

object AppListSerializer : Serializer<List<AppInfo>> {
    // Provide a default value for the DataStore
    override val defaultValue: List<AppInfo> = emptyList()

    // Reading from the DataStore (deserialize the stored data)
    override suspend fun readFrom(input: InputStream): List<AppInfo> {
        return try {
            val jsonString = input.readBytes().decodeToString()
            Json.decodeFromString(jsonString) // Deserialize JSON to List<AppInfo>
        } catch (e: Exception) {
            emptyList() // Return empty list if there's an issue with deserialization
        }
    }

    // Writing to the DataStore (serialize the data)
    override suspend fun writeTo(
        t: List<AppInfo>,
        output: OutputStream,
    ) {
        val jsonString = Json.encodeToString(t) // Serialize List<AppInfo> to JSON
        output.write(jsonString.encodeToByteArray()) // Write to output stream
    }
}
