package dev.hossain.keepalive.data
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_SEC
import dev.hossain.keepalive.util.AppConfig.MINIMUM_APP_CHECK_INTERVAL_MIN
import dev.hossain.keepalive.util.AppConfig.MINIMUM_APP_CHECK_INTERVAL_SEC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

// Define keys for the preferences
private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val APP_CHECK_INTERVAL = intPreferencesKey("app_check_interval")
        val ENABLE_HEALTH_CHECK = booleanPreferencesKey("enable_health_check")
        val HEALTH_CHECK_UUID = stringPreferencesKey("health_check_uuid")
        val ENABLE_REMOTE_LOGGING = booleanPreferencesKey("enable_remote_logging")
        val AIRTABLE_TOKEN = stringPreferencesKey("airtable_token")
        val AIRTABLE_DATA_URL = stringPreferencesKey("airtable_data_url")
    }

    // Retrieve app check interval from DataStore
    val appCheckIntervalFlow: Flow<Int> =
        context.dataStore.data
            .map { preferences ->
                val interval = preferences[APP_CHECK_INTERVAL] ?: DEFAULT_APP_CHECK_INTERVAL_SEC
                if (interval < MINIMUM_APP_CHECK_INTERVAL_SEC) MINIMUM_APP_CHECK_INTERVAL_SEC else interval
            }

    // Retrieve health check enabled state from DataStore
    val enableHealthCheckFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[ENABLE_HEALTH_CHECK] ?: false // Default to disabled
            }

    // Retrieve health check UUID from DataStore
    val healthCheckUUIDFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[HEALTH_CHECK_UUID] ?: "" // Default to empty string
            }

    // Retrieve remote logging enabled state from DataStore
    val enableRemoteLoggingFlow: Flow<Boolean> =
        context.dataStore.data
            .map { preferences ->
                preferences[ENABLE_REMOTE_LOGGING] ?: false // Default to disabled
            }

    // Retrieve Airtable token from DataStore
    val airtableTokenFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[AIRTABLE_TOKEN] ?: "" // Default to empty string
            }

    // Retrieve Airtable data URL from DataStore
    val airtableDataUrlFlow: Flow<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[AIRTABLE_DATA_URL] ?: "" // Default to empty string
            }

    // Combine remote logging enabled state, Airtable token, and Airtable data URL from DataStore
    val airtableConfig: Flow<AirtableConfig> =
        combine(
            enableRemoteLoggingFlow,
            airtableTokenFlow,
            airtableDataUrlFlow,
        ) { isRemoteLoggingEnabled, airtableToken, airtableDataUrl ->
            AirtableConfig(isRemoteLoggingEnabled, airtableToken, airtableDataUrl)
        }

    // Save values in DataStore
    suspend fun saveAppCheckInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[APP_CHECK_INTERVAL] = interval
        }
    }

    suspend fun saveEnableHealthCheck(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_HEALTH_CHECK] = enabled
        }
    }

    suspend fun saveHealthCheckUUID(uuid: String) {
        context.dataStore.edit { preferences ->
            preferences[HEALTH_CHECK_UUID] = uuid
        }
    }

    suspend fun saveEnableRemoteLogging(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_REMOTE_LOGGING] = enabled
        }
    }

    suspend fun saveAirtableToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AIRTABLE_TOKEN] = token
        }
    }

    suspend fun saveAirtableDataUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[AIRTABLE_DATA_URL] = url
        }
    }
}
