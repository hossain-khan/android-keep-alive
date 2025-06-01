package dev.hossain.keepalive.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.ui.screen.AppInfo
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import dev.hossain.keepalive.util.AppConfig.DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS
import dev.hossain.keepalive.util.AppLauncher
import dev.hossain.keepalive.util.HttpPingSender
import dev.hossain.keepalive.util.NotificationHelper
import dev.hossain.keepalive.util.RecentAppChecker
import dev.hossain.keepalive.util.Validator.isValidUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Service that keeps an eye on the apps configured by user and restarts if it's killed.
 *
 * IMPORTANT PERMISSION REQUIREMENTS:
 * For reliable operation, especially on Android 10 (API 29) and above, this service relies on:
 * 1. `android.permission.PACKAGE_USAGE_STATS`: To accurately check if a monitored app is running.
 *    The user must grant this permission through system settings.
 * 2. `android.permission.SYSTEM_ALERT_WINDOW`: To allow the service to launch apps from the background.
 *    The user must grant this permission through system settings.
 * The app should guide the user to grant these permissions if they are not already available.
 *
 * This service runs as a foreground service to ensure it's not killed by the system.
 * It also requires `android.permission.POST_NOTIFICATIONS` on Android 13 (API 33)+ for its foreground notification.
 */
class WatchdogService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val pingSender = HttpPingSender(this)
    private val notificationHelper = NotificationHelper(this)
    private lateinit var activityLogger: AppActivityLogger

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
        activityLogger = AppActivityLogger(applicationContext)

        startForeground(
            NOTIFICATION_ID,
            notificationHelper.buildNotification(),
        )

        val dataStore = AppDataStore.store(context = applicationContext)
        val appSettings = SettingsRepository(applicationContext)

        // Launch a coroutine to monitor the app check interval flow
        // This ensures we always have the most recent interval value
        var currentCheckInterval = DEFAULT_APP_CHECK_INTERVAL_MIN
        serviceScope.launch {
            appSettings.appCheckIntervalFlow.collect { interval ->
                Timber.d("App check interval updated from $currentCheckInterval to $interval minutes")
                currentCheckInterval = interval
            }
        }

        serviceScope.launch {
            // Preloads the initial value of the app check interval
            currentCheckInterval = appSettings.appCheckIntervalFlow.first()

            while (true) {
                Timber.d("[Start ID: $serviceStartId] Current time: ${System.currentTimeMillis()} @ ${Date()}")
                val appsList: List<AppInfo> = dataStore.data.first()

                // Use the latest interval value that's being updated by the collector above
                Timber.d("Scheduling next check using current interval: $currentCheckInterval minutes.")
                delay(TimeUnit.MINUTES.toMillis(currentCheckInterval.toLong()))

                // 👆🏽 Comment above first to disable configured delay 👆🏽
                // - - - - - - - - - - - - - - - - - - - - - - - - - - -
                // For debug/development use smaller value see changes frequently
                // delay(20_000L) // ⛔️ DO NOT COMMIT ⛔️

                if (appsList.isEmpty()) {
                    Timber.w("No apps configured yet. Skipping the check.")
                    continue
                }

                val checkIntervalMillis = TimeUnit.MINUTES.toMillis(currentCheckInterval.toLong())
                val recentlyRunApps = RecentAppChecker.getRecentlyRunningAppStats(this@WatchdogService, checkIntervalMillis)
                val shouldForceStart = appSettings.enableForceStartAppsFlow.first()

                if (shouldForceStart) {
                    Timber.d("Force start apps settings is enabled.")
                } else {
                    Timber.d("Force start apps settings is disabled.")
                }

                appsList.forEach { appInfo ->
                    val isAppRunningRecently = RecentAppChecker.isAppRunningRecently(recentlyRunApps, appInfo.packageName)
                    val needsToStart = !isAppRunningRecently || shouldForceStart

                    // Log app activity regardless of whether the app needs to be started
                    val timestamp = System.currentTimeMillis()
                    val message =
                        if (needsToStart) {
                            if (shouldForceStart) {
                                "Force starting app regardless of running state"
                            } else {
                                "App was not running recently, attempting to start"
                            }
                        } else {
                            "App is running normally, no action needed"
                        }

                    // Create and save the log entry
                    val activityLog =
                        AppActivityLog(
                            packageId = appInfo.packageName,
                            appName = appInfo.appName,
                            wasRunningRecently = isAppRunningRecently,
                            wasAttemptedToStart = needsToStart,
                            timestamp = timestamp,
                            forceStartEnabled = shouldForceStart,
                            message = message,
                        )
                    activityLogger.logAppActivity(activityLog)

                    if (needsToStart) {
                        Timber.d(
                            "[Start ID: $serviceStartId] ${appInfo.appName} app is not running. " +
                                "Attempting to start it now. shouldForceStart=$shouldForceStart",
                        )
                        AppLauncher.openApp(this@WatchdogService, appInfo.packageName)
                    } else {
                        // If app is already running, send health check ping
                        conditionallySendHealthCheck(appSettings)
                    }

                    delay(DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS)
                }
            }
        }

        // Restart the service if it's killed
        return START_STICKY
    }

    private suspend fun conditionallySendHealthCheck(appSettings: SettingsRepository) {
        val healthCheckEnabled = appSettings.enableHealthCheckFlow.first()
        val healthCheckUUID = appSettings.healthCheckUUIDFlow.first()

        if (healthCheckEnabled && isValidUUID(healthCheckUUID)) {
            Timber.d("Health check is enabled. Sending health check ping.")
            pingSender.sendPingToDevice(healthCheckUUID)
        } else {
            Timber.d("Health check is disabled or UUID is not set. Skipping health check ping.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("onDestroy: Service is being destroyed. Service ID: $serviceStartId ($this)")

        // Cancel the scope to clean up resources
        serviceScope.cancel()
    }
}
