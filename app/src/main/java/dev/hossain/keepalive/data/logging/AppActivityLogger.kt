package dev.hossain.keepalive.data.logging

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.keepalive.data.model.AppActivityLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Extension property to provide a [DataStore] instance for app activity logs, named "app_activity_logs". */
private val Context.activityLogDataStore by preferencesDataStore(name = "app_activity_logs")

/**
 * Logger responsible for recording and managing in-device app activity related to monitoring.
 *
 * This class uses Jetpack DataStore (Preferences) to store a list of [AppActivityLog] objects
 * as a JSON string. It provides functionalities to:
 * - Log new app activities ([logAppActivity]).
 * - Retrieve all stored activity logs as a [Flow] ([activityLogs]).
 * - Retrieve a specific number of recent logs ([getRecentLogs]).
 * - Clear all stored logs ([clearLogs]).
 *
 * The logger maintains a maximum number of log entries ([MAX_LOGS]) to prevent excessive storage usage.
 *
 * @param context The application context, used to access the DataStore.
 */
class AppActivityLogger constructor(private val context: Context) {
    companion object {
        /** The maximum number of log entries to store. Older entries are discarded beyond this limit. */
        private const val MAX_LOGS = 300

        /** [Preferences.Key] for storing the JSON string of app activity logs. */
        private val ACTIVITY_LOGS_KEY = stringPreferencesKey("app_activity_logs")

        /** Configured [Json] instance for serialization and deserialization, ignoring unknown keys for forward compatibility. */
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * A [Flow] that emits the current list of [AppActivityLog] objects retrieved from DataStore.
     * The logs are stored as a JSON string, which is deserialized here.
     * If deserialization fails, an empty list is emitted and an error is logged.
     */
    val activityLogs: Flow<List<AppActivityLog>> =
        context.activityLogDataStore.data.map { preferences ->
            val logsJson = preferences[ACTIVITY_LOGS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<AppActivityLog>>(logsJson)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode app activity logs")
                emptyList()
            }
        }

    /**
     * Logs a new [AppActivityLog] entry to DataStore.
     *
     * The new log entry is prepended to the existing list of logs.
     * The list is then truncated to ensure that the total number of stored logs
     * does not exceed [MAX_LOGS]. The updated list is serialized to JSON and saved.
     *
     * @param log The [AppActivityLog] entry to be added.
     */
    suspend fun logAppActivity(log: AppActivityLog) {
        context.activityLogDataStore.edit { preferences ->
            val existingLogsJson = preferences[ACTIVITY_LOGS_KEY] ?: "[]"
            val existingLogs =
                try {
                    json.decodeFromString<List<AppActivityLog>>(existingLogsJson)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to decode existing logs, creating new list")
                    emptyList()
                }

            // Add new log to the beginning and limit to MAX_LOGS
            val updatedLogs = (listOf(log) + existingLogs).take(MAX_LOGS)
            preferences[ACTIVITY_LOGS_KEY] = json.encodeToString(updatedLogs)
        }
    }

    /**
     * Clears all stored app activity logs from DataStore.
     * This is achieved by setting the log data to an empty JSON array string.
     */
    suspend fun clearLogs() {
        context.activityLogDataStore.edit { preferences ->
            preferences[ACTIVITY_LOGS_KEY] = "[]"
        }
    }

    /**
     * Retrieves a [Flow] that emits a list of the most recent [AppActivityLog] entries,
     * limited by the specified [count].
     *
     * If [count] is not provided, it defaults to [MAX_LOGS].
     * This method utilizes the main [activityLogs] Flow and applies a `take` operation.
     *
     * @param count The maximum number of recent log entries to retrieve. Defaults to [MAX_LOGS].
     * @return A [Flow] emitting a list of the most recent [AppActivityLog] entries.
     */
    fun getRecentLogs(count: Int = MAX_LOGS): Flow<List<AppActivityLog>> {
        return activityLogs.map { logs ->
            logs.take(count)
        }
    }
}
