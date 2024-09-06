package dev.hossain.keepalive.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.util.AppChecker
import dev.hossain.keepalive.util.AppLauncher
import dev.hossain.keepalive.util.HttpPingSender
import dev.hossain.keepalive.util.NotificationHelper
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
 * Service that keeps an eye on the Google Photos and Sync app and restarts if it's killed.
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
        notificationHelper.createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            notificationHelper.buildNotification(),
        )

        val dataStore = AppDataStore.store(context = applicationContext)

        serviceScope.launch {
            val appsList = dataStore.data.first()
            Timber.d("DataStore State: $appsList")

            while (true) {
                Timber.d("Current time: " + System.currentTimeMillis() + " @ " + Date())
                delay(CHECK_INTERVAL_MILLIS)

                if (!AppChecker.isGooglePhotosRunning(this@WatchdogService)) {
                    Timber.d("Photos app is not running. Starting it now.")
                    AppLauncher.openGooglePhotos(this@WatchdogService)
                } else {
                    Timber.d("Photos app is already running.")
                    pingSender.sendPingToDevice()
                }

                delay(30_000L) // 30 seconds
                if (!AppChecker.isSyncthingRunning(this@WatchdogService)) {
                    Timber.d("Sync app is not running. Starting it now.")
                    AppLauncher.openSyncthing(this@WatchdogService)
                } else {
                    Timber.d("Sync app is already running.")
                    pingSender.sendPingToDevice()
                }

                appsList.forEach {
                    if (!AppChecker.isAppRunning(this@WatchdogService, it.packageName)) {
                        Timber.d("${it.appName} app is not running. Starting it now.")
                        AppLauncher.openApp(this@WatchdogService, it.packageName)
                    } else {
                        Timber.d("${it.appName} app is already running.")
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

        // Cancel the scope to clean up resources
        serviceScope.cancel()
    }
}
