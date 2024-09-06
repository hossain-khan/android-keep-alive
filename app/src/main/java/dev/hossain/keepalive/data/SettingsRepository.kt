package dev.hossain.keepalive.data
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define keys for the preferences
private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val APP_CHECK_INTERVAL = intPreferencesKey("app_check_interval")
        val ENABLE_HEALTH_CHECK = booleanPreferencesKey("enable_health_check")
        val HEALTH_CHECK_UUID = stringPreferencesKey("health_check_uuid")
    }

    // Retrieve app check interval from DataStore
    val appCheckIntervalFlow: Flow<Int> =
        context.dataStore.data
            .map { preferences ->
                preferences[APP_CHECK_INTERVAL] ?: DEFAULT_APP_CHECK_INTERVAL
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
}
