package dev.hossain.keepalive.util

import android.content.Context
import android.content.Intent
import android.os.Build
import dev.hossain.keepalive.service.WatchdogService
import timber.log.Timber

/**
 * Utility object for managing the [WatchdogService].
 *
 * Provides methods to start and restart the [WatchdogService].
 * It ensures that the service is started as a foreground service.
 */
object ServiceManager {
    /**
     * Starts the [WatchdogService] if it is not already running.
     *
     * This method first checks [WatchdogService.isRunning].
     * If not running, it creates an intent for [WatchdogService] and starts it as a foreground service
     * using `context.startForegroundService()` on API 26+ or `context.startService()` on older versions.
     * This is crucial for long-running background tasks.
     *
     * @param context The [Context] used to start the service.
     */
    fun startWatchdogService(context: Context) {
        Timber.d("Attempting to start WatchdogService if not already running.")
        if (!WatchdogService.isRunning) {
            Timber.i("WatchdogService is not running. Starting it now as a foreground service.")
            val serviceIntent = Intent(context, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            Timber.d("WatchdogService is already running. No action needed.")
        }
    }

    /**
     * Restarts the [WatchdogService].
     *
     * This is typically called when configuration changes need to be applied to the service,
     * as services often read their configuration upon starting.
     * The method first stops the service if it's running, then starts it again as a foreground service
     * on API 26+ or as a regular service on older versions.
     *
     * @param context The [Context] used to stop and start the service.
     */
    fun restartWatchdogService(context: Context) {
        Timber.i("Restarting WatchdogService to apply new configuration.")
        val serviceIntent = Intent(context, WatchdogService::class.java)

        // Stop the service if it's running
        if (WatchdogService.isRunning) {
            Timber.d("Stopping existing WatchdogService instance.")
            context.stopService(serviceIntent)
        } else {
            Timber.d("WatchdogService was not running. Will proceed to start it.")
        }

        // Start the service again
        Timber.d("Starting new WatchdogService instance as a foreground service.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
