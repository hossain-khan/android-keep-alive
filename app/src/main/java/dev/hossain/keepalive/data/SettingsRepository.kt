package dev.hossain.keepalive.data
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import dev.hossain.keepalive.util.AppConfig.MINIMUM_APP_CHECK_INTERVAL_MIN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Define keys for the preferences
private val Context.dataStore by preferencesDataStore(name = "app_settings")

/**
 * Manages application settings using DataStore.
 */
class SettingsRepository(private val context: Context) {
    companion object {
        val APP_CHECK_INTERVAL = intPreferencesKey("app_check_interval")
        val ENABLE_FORCE_START_APPS = booleanPreferencesKey("enable_force_start_apps")
        val ENABLE_HEALTH_CHECK = booleanPreferencesKey("enable_health_check")
        val HEALTH_CHECK_UUID = stringPreferencesKey("health_check_uuid")
        val ENABLE_REMOTE_LOGGING = booleanPreferencesKey("enable_remote_logging")
        val AIRTABLE_TOKEN = stringPreferencesKey("airtable_token")
        val AIRTABLE_DATA_URL = stringPreferencesKey("airtable_data_url")
    }

    /**
     * Flow representing the app check interval in minutes.
     */
    val appCheckIntervalFlow: Flow<Int> =
        context.dataStore.data
            .map { preferences ->
                val interval = preferences[APP_CHECK_INTERVAL] ?: DEFAULT_APP_CHECK_INTERVAL_MIN
                if (interval < MINIMUM_APP_CHECK_INTERVAL_MIN) MINIMUM_APP_CHECK_INTERVAL_MIN else interval
            }

    /**
     * Flow representing whether force starting apps is enabled.
     */
    val enableForceStartAppsFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[ENABLE_FORCE_START_APPS] ?: false // Default to disabled
            }

    /**
     * Flow representing whether health check is enabled.
     */
    val enableHealthCheckFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[ENABLE_HEALTH_CHECK] ?: false // Default to disabled
            }

    /**
     * Flow representing the health check UUID.
     */
    val healthCheckUUIDFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[HEALTH_CHECK_UUID] ?: "" // Default to empty string
            }

    /**
     * Flow representing whether remote logging is enabled.
     */
    val enableRemoteLoggingFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[ENABLE_REMOTE_LOGGING] ?: false // Default to disabled
            }

    /**
     * Flow representing the Airtable token.
     */
    val airtableTokenFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[AIRTABLE_TOKEN] ?: "" // Default to empty string
            }

    /**
     * Flow representing the Airtable data URL.
     */
    val airtableDataUrlFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[AIRTABLE_DATA_URL] ?: "" // Default to empty string
            }

    /**
     * Flow representing the Airtable configuration.
     */
    val airtableConfig: Flow<AirtableConfig> =
        combine(
            enableRemoteLoggingFlow,
            airtableTokenFlow,
            airtableDataUrlFlow,
        ) { isRemoteLoggingEnabled, airtableToken, airtableDataUrl ->
            AirtableConfig(isRemoteLoggingEnabled, airtableToken, airtableDataUrl)
        }

    /**
     * Saves the app check interval in minutes.
     */
    suspend fun saveAppCheckInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[APP_CHECK_INTERVAL] = interval
        }
    }

    /**
     * Saves whether force starting apps is enabled.
     */
    suspend fun saveEnableForceStartApps(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_FORCE_START_APPS] = enabled
        }
    }

    /**
     * Saves whether health check is enabled.
     */
    suspend fun saveEnableHealthCheck(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_HEALTH_CHECK] = enabled
        }
    }

    /**
     * Saves the health check UUID.
     */
    suspend fun saveHealthCheckUUID(uuid: String) {
        context.dataStore.edit { preferences ->
            preferences[HEALTH_CHECK_UUID] = uuid
        }
    }

    /**
     * Saves whether remote logging is enabled.
     */
    suspend fun saveEnableRemoteLogging(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_REMOTE_LOGGING] = enabled
        }
    }

    /**
     * Saves the Airtable token.
     */
    suspend fun saveAirtableToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AIRTABLE_TOKEN] = token
        }
    }

    /**
     * Saves the Airtable data URL.
     */
    suspend fun saveAirtableDataUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[AIRTABLE_DATA_URL] = url
        }
    }
}
