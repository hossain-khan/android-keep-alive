package dev.hossain.keepalive.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import dev.hossain.keepalive.service.WatchdogService
import timber.log.Timber

/**
 * Utility class to manage services in the application.
 */
object ServiceManager {
    /**
     * Restarts the WatchdogService to apply new configuration settings.
     *
     * @param context The context to use for service operations
     */
    fun restartWatchdogService(context: Context) {
        Timber.d("Restarting WatchdogService to apply new configuration")
        val serviceIntent = Intent(context, WatchdogService::class.java)

        // Stop the service if it's running
        if (isServiceRunning(context, WatchdogService::class.java)) {
            Timber.d("Stopping existing WatchdogService")
            context.stopService(serviceIntent)
        }

        // Start the service again
        Timber.d("Starting WatchdogService with new configuration")
        context.startForegroundService(serviceIntent)
    }

    /**
     * Checks if a service is currently running.
     *
     * @param context The context to use for checking service status
     * @param serviceClass The class of the service to check
     * @return True if the service is running, false otherwise
     */
    @Suppress("DEPRECATION")
    private fun isServiceRunning(
        context: Context,
        serviceClass: Class<*>,
    ): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // DEPRECATED: `getRunningServices()` method is no longer available to third party applications.
        // For backwards compatibility, it will still return the caller's own services.
        // So, this method is still useful for detecting if your own services are running.
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
