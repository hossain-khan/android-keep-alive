package dev.hossain.keepalive.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import dev.hossain.keepalive.service.WatchdogService
import timber.log.Timber

/**
 * Utility object for managing the [WatchdogService].
 *
 * Provides methods to start, restart, and check the running status of the [WatchdogService].
 * It ensures that the service is started as a foreground service.
 */
object ServiceManager {
    /**
     * Starts the [WatchdogService] if it is not already running.
     *
     * This method first checks if the service is currently running using [isServiceRunning].
     * If not, it creates an intent for [WatchdogService] and starts it as a foreground service
     * using `context.startForegroundService()`. This is crucial for long-running background tasks.
     *
     * @param context The [Context] used to start the service and check its status.
     */
    fun startWatchdogService(context: Context) {
        Timber.d("Attempting to start WatchdogService if not already running.")
        if (!isServiceRunning(context, WatchdogService::class.java)) {
            Timber.i("WatchdogService is not running. Starting it now as a foreground service.")
            val serviceIntent = Intent(context, WatchdogService::class.java)
            context.startForegroundService(serviceIntent)
        } else {
            Timber.d("WatchdogService is already running. No action needed.")
        }
    }

    /**
     * Restarts the [WatchdogService].
     *
     * This is typically called when configuration changes need to be applied to the service,
     * as services often read their configuration upon starting.
     * The method first stops the service if it's running, then starts it again as a foreground service.
     *
     * @param context The [Context] used to stop and start the service.
     */
    fun restartWatchdogService(context: Context) {
        Timber.i("Restarting WatchdogService to apply new configuration.")
        val serviceIntent = Intent(context, WatchdogService::class.java)

        // Stop the service if it's running
        if (isServiceRunning(context, WatchdogService::class.java)) {
            Timber.d("Stopping existing WatchdogService instance.")
            context.stopService(serviceIntent)
        } else {
            Timber.d("WatchdogService was not running. Will proceed to start it.")
        }

        // Start the service again
        Timber.d("Starting new WatchdogService instance as a foreground service.")
        context.startForegroundService(serviceIntent)
    }

    /**
     * Checks if a specific service class is currently running.
     *
     * It uses [ActivityManager.getRunningServices] to iterate through running services.
     *
     * Important Note on `getRunningServices()`:
     * As of Android API level 26 (Oreo) and higher, this method is deprecated for retrieving
     * information about services run by other applications. However, for services within
     * the calling application's own package (like [WatchdogService] here), it still works.
     * The `@Suppress("DEPRECATION")` annotation is used because of this specific use case.
     *
     * @param context The [Context] to access the [ActivityManager].
     * @param serviceClass The [Class] object of the service to check (e.g., `WatchdogService::class.java`).
     * @return `true` if the service is found in the list of running services, `false` otherwise.
     */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(
        context: Context,
        serviceClass: Class<*>,
    ): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
                ?: return false // Should not happen, but good practice to check

        // DEPRECATED: `getRunningServices()` method is no longer available to third party applications.
        // For backwards compatibility, it will still return the caller's own services.
        // So, this method is still useful for detecting if your own services are running.
        try {
            for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: SecurityException) {
            // Catch SecurityException, which might occur on some modified ROMs.
            Timber.e(e, "SecurityException while checking if service ${serviceClass.name} is running.")
            return false // Assume not running if a security error occurs
        } catch (e: RuntimeException) {
            // Catch other runtime exceptions that might occur unexpectedly.
            Timber.e(e, "RuntimeException while checking if service ${serviceClass.name} is running.")
            return false // Assume not running if a runtime error occurs
        }
        return false
    }
}
