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

// Create a single DataStore instance as a companion object property
private val Context.activityLogDataStore by preferencesDataStore(name = "app_activity_logs")

/**
 * Logger for in-device app activity monitoring.
 *
 * This class handles storing app activity logs in preferences and provides
 * methods to retrieve and manage these logs.
 */
class AppActivityLogger constructor(private val context: Context) {
    companion object {
        private const val MAX_LOGS = 300
        private val ACTIVITY_LOGS_KEY = stringPreferencesKey("app_activity_logs")
        private val json =
            if (BuildConfig.DEBUG) {
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                }
            } else {
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                }
            }
    }

    /**
     * Flow of app activity logs retrieved from preferences.
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
     * Logs a new app activity entry.
     *
     * Adds the log entry to the beginning of the list and ensures the total
     * number of logs doesn't exceed [MAX_LOGS].
     *
     * @param log The app activity log entry to add
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
     * Clears all stored activity logs.
     */
    suspend fun clearLogs() {
        context.activityLogDataStore.edit { preferences ->
            preferences[ACTIVITY_LOGS_KEY] = "[]"
        }
    }

    /**
     * Gets the most recent log entries, limited by count.
     *
     * @param count Maximum number of logs to retrieve (defaults to MAX_LOGS)
     * @return Flow of the most recent logs
     */
    fun getRecentLogs(count: Int = MAX_LOGS): Flow<List<AppActivityLog>> {
        return activityLogs.map { logs ->
            logs.take(count)
        }
    }
}
