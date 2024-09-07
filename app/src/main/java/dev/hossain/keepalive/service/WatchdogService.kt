package dev.hossain.keepalive.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_PACKAGE_NAME
import dev.hossain.keepalive.util.AppConfig.SYNC_APP_PACKAGE_NAME
import dev.hossain.keepalive.util.AppLauncher
import dev.hossain.keepalive.util.HttpPingSender
import dev.hossain.keepalive.util.NotificationHelper
import dev.hossain.keepalive.util.RecentAppChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

/**
 * Service that keeps an eye on the apps configured by user and restarts if it's killed.
 */
class WatchdogService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1

        private const val CHECK_INTERVAL_MILLIS = 1800_000L // 30 minutes x2

        // Less time for debugging - 20 seconds
        // private const val CHECK_INTERVAL_MILLIS = 20_000L
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val pingSender = HttpPingSender(this)
    private val notificationHelper = NotificationHelper(this)

    // Used to tracking the instance of the service
    private var serviceStartId: Int? = null

    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind: $intent")
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.d("onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId")
        serviceStartId = startId
        notificationHelper.createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            notificationHelper.buildNotification(),
        )

        val dataStore = AppDataStore.store(context = applicationContext)
        val appSettings = SettingsRepository(applicationContext)

        serviceScope.launch {
            val appsList = dataStore.data.first()
            Timber.d("DataStore State: $appsList")

            while (true) {
                Timber.d("[Start ID: $serviceStartId] Current time: " + System.currentTimeMillis() + " @ " + Date())
                appSettings.appCheckIntervalFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] App check interval: $it minutes")
                }
                appSettings.healthCheckUUIDFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] Health check UUID: $it")
                }
                appSettings.enableHealthCheckFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] Health check enabled: $it")
                }
                appSettings.enableRemoteLoggingFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] Remote logging enabled: $it")
                }
                appSettings.airtableTokenFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] Airtable token: $it")
                }
                appSettings.airtableDataUrlFlow.first().let {
                    Timber.d("[Start ID: $serviceStartId] Airtable data URL: $it")
                }

                delay(CHECK_INTERVAL_MILLIS)

                val recentlyRunApps = RecentAppChecker.getRecentlyRunningAppStats(this@WatchdogService)

                if (!RecentAppChecker.isAppRunningRecently(recentlyRunApps, PHOTOS_APP_PACKAGE_NAME)) {
                    Timber.d("Photos app is not running. Starting it now.")
                    AppLauncher.openGooglePhotos(this@WatchdogService)
                } else {
                    Timber.d("Photos app is already running.")
                    pingSender.sendPingToDevice()
                }

                delay(30_000L) // 30 seconds
                if (!RecentAppChecker.isAppRunningRecently(recentlyRunApps, SYNC_APP_PACKAGE_NAME)) {
                    Timber.d("Sync app is not running. Starting it now.")
                    AppLauncher.openSyncthing(this@WatchdogService)
                } else {
                    Timber.d("Sync app is already running.")
                    pingSender.sendPingToDevice()
                }

                appsList.forEach {
                    if (!RecentAppChecker.isAppRunningRecently(recentlyRunApps, it.packageName)) {
                        Timber.d("[Start ID: $serviceStartId] ${it.appName} app is not running. Starting it now.")
                        AppLauncher.openApp(this@WatchdogService, it.packageName)
                    } else {
                        Timber.d("[Start ID: $serviceStartId] ${it.appName} app is already running.")
                        pingSender.sendPingToDevice()
                    }

                    delay(10_000L) // 10 seconds
                }

                delay(CHECK_INTERVAL_MILLIS)
            }
        }

        // Restart the service if it's killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("onDestroy: Service is being destroyed. Service ID: $serviceStartId ($this)")

        // Cancel the scope to clean up resources
        serviceScope.cancel()
    }
}
