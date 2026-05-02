package dev.hossain.keepalive.log

import android.os.Build
import android.util.Log
import dev.hossain.keepalive.BuildConfig
import org.json.JSONObject

/**
 * Cell name for saving device information. Cell type: single line text
 */
private const val COLUMN_NAME_DEVICE = "Device"

/**
 * Cell name for saving log message. Cell type: long text
 */
private const val COLUMN_NAME_LOG = "Log"

/**
 * Represents a single log message that will be sent to a remote API (e.g., Airtable).
 *
 * @property priority The Android log priority level (e.g., [android.util.Log.DEBUG]).
 * @property tag Optional log tag associated with the message.
 * @property message The log message content.
 * @property throwable Optional [Throwable] associated with this log entry.
 * @property logSequence A monotonically increasing sequence number for ordering log messages.
 * @property logTime The timestamp in milliseconds when this log message was created.
 * @property device The device model name, used to identify the source device in remote logs.
 */
data class LogMessage(
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwable: Throwable?,
    val logSequence: Int,
    val logTime: Long = System.currentTimeMillis(),
    val device: String = Build.MODEL,
) {
    /**
     * Converts this [LogMessage] into an Airtable-compatible [JSONObject] record.
     *
     * The resulting JSON object follows the Airtable "Create Records" API format,
     * with a `"fields"` key containing the [COLUMN_NAME_DEVICE] and [COLUMN_NAME_LOG] values.
     *
     * @return A [JSONObject] representing one Airtable record for this log message.
     */
    fun toLogRecord(): JSONObject {
        val logMessage =
            buildString {
                append("Priority: ${priority.toLogType()}\n")
                if (tag != null) {
                    append("Tag: $tag\n")
                }
                append("Message: $message\n")
                if (throwable != null) {
                    append("Throwable: ${throwable.localizedMessage}")
                }
                append("App Version: ${BuildConfig.VERSION_NAME}\n")

                append("Log Time: ${logTime}\n")

                append("Log Sequence: $logSequence\n")
            }

        val fields =
            JSONObject().apply {
                put(COLUMN_NAME_DEVICE, device)
                put(COLUMN_NAME_LOG, logMessage)
            }
        return JSONObject().apply {
            put("fields", fields)
        }
    }

    /**
     * Maps an Android [android.util.Log] priority integer to its string representation.
     *
     * @return A human-readable string such as "DEBUG", "INFO", "WARN", "ERROR", etc.
     */
    private fun Int.toLogType(): String {
        return when (this) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
    }
}
