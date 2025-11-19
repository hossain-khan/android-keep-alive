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

data class LogMessage(
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwable: Throwable?,
    val logSequence: Int,
    val logTime: Long = System.currentTimeMillis(),
    val device: String = Build.MODEL,
) {
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
