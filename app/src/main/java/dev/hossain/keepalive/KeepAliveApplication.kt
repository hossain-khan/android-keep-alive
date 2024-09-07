package dev.hossain.keepalive

import android.app.Application
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.log.ApiLoggingTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class KeepAliveApplication : Application() {
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        val settingsRepository = SettingsRepository(this)
        applicationScope.launch {
            settingsRepository.airtableConfig.first { airtableConfig ->
                if (airtableConfig.isValid()) {
                    Timber.d("Airtable configuration is valid. Planting remote logging tree.")
                    Timber.plant(ApiLoggingTree(airtableConfig.token, airtableConfig.dataUrl))
                } else {
                    Timber.d("Airtable configuration is invalid or not set. Skipping remote logging tree.")
                }
                return@first false
            }
        }

        Timber.i("KeepAliveApplication.onCreate() completed.")
    }
}
