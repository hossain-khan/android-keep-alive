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
                    val isApiLoggingTreePlanted = Timber.forest().any { it is ApiLoggingTree }
                    if (!isApiLoggingTreePlanted) {
                        Timber.d("Airtable configuration is valid. Planting remote logging tree.")
                        Timber.plant(
                            ApiLoggingTree(
                                isEnabled = airtableConfig.isValid(),
                                authToken = airtableConfig.token,
                                endpointUrl = airtableConfig.dataUrl,
                            ),
                        )
                    } else {
                        Timber.d("ApiLoggingTree is already planted. Skipping planting.")
                    }
                } else {
                    Timber.d("Airtable configuration is invalid or not enabled. Disable remote logging.")
                    val apiLoggingTree = Timber.forest().firstOrNull { it is ApiLoggingTree }
                    apiLoggingTree?.let {
                        Timber.uproot(it)
                    }
                }
                return@first false
            }
        }

        Timber.i("KeepAliveApplication.onCreate() completed.")
    }
}
