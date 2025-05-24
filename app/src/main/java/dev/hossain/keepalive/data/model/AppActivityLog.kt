package dev.hossain.keepalive.data.model

import androidx.annotation.Keep
import dev.hossain.keepalive.service.WatchdogService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model class that represents a single log entry for app activity monitoring.
 *
 * This captures the state of an app when checked by the [WatchdogService].
 */
@Keep
@Serializable
data class AppActivityLog(
    /**
     * Package ID of the application being monitored
     */
    @SerialName("packageId")
    val packageId: String,
    /**
     * Human-readable name of the application
     */
    @SerialName("appName")
    val appName: String,
    /**
     * Whether the app was detected as recently running when checked
     */
    @SerialName("wasRunningRecently")
    val wasRunningRecently: Boolean,
    /**
     * Whether the app was attempted to be started
     */
    @SerialName("wasAttemptedToStart")
    val wasAttemptedToStart: Boolean,
    /**
     * Unix timestamp (milliseconds) when this check was performed
     */
    @SerialName("timestamp")
    val timestamp: Long,
    /**
     * Whether force start app setting was enabled at the time of check
     */
    @SerialName("forceStartEnabled")
    val forceStartEnabled: Boolean,
    /**
     * Optional message with additional details about the operation
     */
    @SerialName("message")
    val message: String = "",
) {
    /**
     * Converts the timestamp to a human-readable date string.
     */
    fun getFormattedTimestamp(): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Returns a simplified status message based on the log data.
     */
    fun getStatusSummary(): String {
        return when {
            wasAttemptedToStart && forceStartEnabled -> "Force started"
            wasAttemptedToStart -> "Started (was not running)"
            wasRunningRecently -> "Running normally"
            else -> "Unknown state"
        }
    }
}
