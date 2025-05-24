package dev.hossain.keepalive.data.model

import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Model class that represents a single log entry for app activity monitoring.
 *
 * This captures the state of an app when checked by the [WatchdogService].
 */
@Serializable
data class AppActivityLog(
    /**
     * Package ID of the application being monitored
     */
    val packageId: String,
    /**
     * Human-readable name of the application
     */
    val appName: String,
    /**
     * Whether the app was detected as recently running when checked
     */
    val wasRunningRecently: Boolean,
    /**
     * Whether the app was attempted to be started
     */
    val wasAttemptedToStart: Boolean,
    /**
     * Unix timestamp (milliseconds) when this check was performed
     */
    val timestamp: Long,
    /**
     * Whether force start app setting was enabled at the time of check
     */
    val forceStartEnabled: Boolean,
    /**
     * Optional message with additional details about the operation
     */
    val message: String = "",
) {
    /**
     * Converts the timestamp to a human-readable date string.
     */
    fun getFormattedTimestamp(): String {
        return Date(timestamp).toString()
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
