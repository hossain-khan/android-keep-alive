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
 * A background service responsible for monitoring user-configured applications.
 *
 * The `WatchdogService` periodically checks if the selected applications are running.
 * If an application is found not to be running, the service will attempt to restart it.
 * This service runs in the foreground to prevent the Android system from killing it,
 * displaying a persistent notification to the user.
 *
 * Key functionalities include:
 * - Monitoring a list of applications defined by the user.
 * - Periodically checking the running state of these applications.
 * - Restarting applications that are not running or have been killed.
 * - Optionally, force-starting applications regardless of their recent running state.
 * - Logging application monitoring activities.
 * - Optionally, sending health check pings to a predefined URL if an app is running.
 *
 * The service utilizes [AppDataStore] for accessing the list of monitored apps and
 * [SettingsRepository] for configuration settings like check interval and health check details.
 * It uses [RecentAppChecker] to determine app running status and [AppLauncher] to restart apps.
 * Notifications are handled by [NotificationHelper].
 */
class WatchdogService : Service() {
    companion object {
        /** Notification ID for the foreground service notification. */
        private const val NOTIFICATION_ID = 1
    }

    /** Job for managing coroutines within this service. Using a SupervisorJob to prevent failure of one child from affecting others. */
    private val serviceJob = SupervisorJob()

    /** Coroutine scope tied to the service's lifecycle and the [serviceJob]. Uses `Dispatchers.Main` for UI-related tasks if any, though primarily background. */
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    /** Utility class instance for sending HTTP pings for health checks. */
    private val pingSender = HttpPingSender(this)

    /** Utility class instance for creating and managing notifications. */
    private val notificationHelper = NotificationHelper(this)

    /** Logger for recording app activity related to the watchdog service. */
    private lateinit var activityLogger: AppActivityLogger

    /**
     * Unique ID for the current instance of the service, provided by `onStartCommand`.
     * This helps in tracking logs specific to a particular service start instance, especially when the service restarts.
     */
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
     * Called by the system every time a client explicitly starts the service by calling `startService(Intent)`.
     * This method is the main entry point for the service's operations.
     *
     * Responsibilities:
     * - Initializes `currentServiceInstanceId` with the `startId` for tracking.
     * - Creates a notification channel (if not already created) and starts the service in the foreground
     *   to ensure it's not killed by the system.
     * - Initializes `activityLogger` for logging app monitoring activities.
     * - Retrieves application settings, including the app check interval and force start preference.
     * - Launches a coroutine that periodically checks the status of configured applications:
     *   - It fetches the list of apps to monitor from [AppDataStore].
     *   - It waits for the configured `currentCheckInterval`.
     *   - It checks if each monitored app was recently running using [RecentAppChecker].
     *   - If an app is not running (or if `shouldForceStart` is true), it attempts to launch it using [AppLauncher].
     *   - Logs the activity using [AppActivityLogger].
     *   - If an app is running, it may send a health check ping via [conditionallySendHealthCheck].
     *   - A small delay ([DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS]) is introduced between checking multiple apps.
     * - The service returns [START_STICKY] to ensure it's restarted by the system if killed.
     *
     * @param intent The Intent supplied to `startService(Intent)`, may contain additional data.
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start. This ID is used
     *                to identify the command instance and is stored in `currentServiceInstanceId`.
     * @return The return value ([START_STICKY]) indicates that the system should recreate the service
     *         if it's killed, and redeliver the last intent.
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
                val monitoredApps: List<AppInfo> = dataStore.data.first()

                // Use the latest interval value that's being updated by the collector above
                Timber.d("[Instance ID: $currentServiceInstanceId] Scheduling next check in $currentCheckInterval minutes.")
                delay(TimeUnit.MINUTES.toMillis(currentCheckInterval.toLong()))

                // 👆🏽 Comment above first to disable configured delay 👆🏽
                // - - - - - - - - - - - - - - - - - - - - - - - - - - -
                // For debug/development use smaller value see changes frequently
                // delay(20_000L) // ⛔️ DO NOT COMMIT ⛔️

                if (monitoredApps.isEmpty()) {
                    Timber.w("[Instance ID: $currentServiceInstanceId] No apps configured yet. Skipping the check.")
                    continue
                }

                val recentlyUsedAppStats = RecentAppChecker.getRecentlyRunningAppStats(this@WatchdogService)
                val shouldForceStart = appSettings.enableForceStartAppsFlow.first()

                if (shouldForceStart) {
                    Timber.d("[Instance ID: $currentServiceInstanceId] Force start apps setting is enabled.")
                } else {
                    Timber.d("[Instance ID: $currentServiceInstanceId] Force start apps setting is disabled.")
                }

                monitoredApps.forEach { appInfo ->
                    val isAppRunningRecently = RecentAppChecker.isAppRunningRecently(recentlyUsedAppStats, appInfo.packageName)
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

                // After processing all monitored apps, launch the sticky app to bring it to the top of recent apps
                val stickyApp = monitoredApps.find { it.isSticky }
                if (stickyApp != null) {
                    Timber.d(
                        "[Instance ID: $currentServiceInstanceId] Launching sticky app ${stickyApp.appName} " +
                            "to bring it to the top of recent apps list.",
                    )

                    // Small delay before launching sticky app to ensure other apps have finished launching
                    delay(DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS)

                    AppLauncher.openApp(this@WatchdogService, stickyApp.packageName)

                    // Log the sticky app activity
                    val stickyActivityLog =
                        AppActivityLog(
                            packageId = stickyApp.packageName,
                            appName = stickyApp.appName,
                            wasRunningRecently = true,
                            wasAttemptedToStart = true,
                            timestamp = System.currentTimeMillis(),
                            forceStartEnabled = shouldForceStart,
                            message = "Sticky app launched to bring it to top of recent apps list",
                        )
                    activityLogger.logAppActivity(stickyActivityLog)
                } else {
                    Timber.d("[Instance ID: $currentServiceInstanceId] No sticky app configured.")
                }
            }
        }

        // Restart the service if it's killed
        return START_STICKY
    }

    /**
     * Conditionally sends a health check ping using [HttpPingSender].
     *
     * This function checks if health checks are enabled and a valid Health Check UUID is configured
     * in the [SettingsRepository]. If both conditions are met, it triggers [HttpPingSender.sendPingToDevice]
     * with the configured UUID.
     *
     * This is typically called when a monitored app is found to be running, as an indication
     * that the watchdog service itself is operational and the monitored app is alive.
     *
     * @param appSettings The [SettingsRepository] instance used to access current health check settings,
     *                    such as whether health checks are enabled and the configured UUID.
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
