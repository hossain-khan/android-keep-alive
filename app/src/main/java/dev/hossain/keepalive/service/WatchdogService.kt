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

    /** Unique ID for the current instance of the service, provided by `onStartCommand`. */
    private var currentServiceInstanceId: Int = 0

    /**
     * Called when a component attempts to bind to the service.
     * This service does not support binding, so it returns null.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the service.
     * Return null if clients cannot bind to the service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        Timber.d("onBind: $intent")
        return null
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * `startService(Intent)`, providing the arguments it supplied and a unique integer token
     * representing the start request.
     *
     * This method initializes the service, sets up notifications, and starts the main monitoring loop.
     *
     * @param intent The Intent supplied to `startService(Intent)`.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's
     * current started state.
     */
    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.d("onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId")
        currentServiceInstanceId = startId
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
                Timber.d("[Instance ID: $currentServiceInstanceId] Current time: ${System.currentTimeMillis()} @ ${Date()}")
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

                val recentlyRunApps = RecentAppChecker.getRecentlyRunningAppStats(this@WatchdogService)
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
                            "[Instance ID: $currentServiceInstanceId] ${appInfo.appName} app is not running. " +
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

    /**
     * Sends a health check ping if health checks are enabled and a valid UUID is configured.
     * This function checks the current settings via [appSettings] and uses [pingSender]
     * to dispatch the ping.
     *
     * @param appSettings The [SettingsRepository] instance to access health check settings.
     */
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

        Timber.d("onDestroy: Service is being destroyed. Service ID: $currentServiceInstanceId ($this)")

        // Cancel the scope to clean up resources
        serviceScope.cancel()
    }
}
