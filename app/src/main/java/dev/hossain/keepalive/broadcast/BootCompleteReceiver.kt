package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.ui.screen.AppInfo
import dev.hossain.keepalive.util.AppLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

/**
 * Receiver to start the WatchdogService on boot complete.
 * Optionally launches configured apps immediately after boot if the setting is enabled.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.d("BootCompleteReceiver onReceive() called with: context = $context, intent = $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Timber.d("BootCompleteReceiver onReceive: Processing boot event")

            // Start the WatchdogService for periodic monitoring
            Timber.d("BootCompleteReceiver onReceive: Starting WatchdogService")
            val serviceIntent = Intent(context, WatchdogService::class.java)
            context.startForegroundService(serviceIntent)

            // Check if we should launch apps immediately on boot
            val settingsRepository = SettingsRepository(context)
            val activityLogger = AppActivityLogger(context)

            receiverScope.launch {
                val launchOnBoot = settingsRepository.launchAppsOnBootFlow.first()
                Timber.d("BootCompleteReceiver: Launch apps on boot setting = $launchOnBoot")

                if (launchOnBoot) {
                    Timber.d("BootCompleteReceiver: Launching apps immediately after boot")
                    launchConfiguredApps(context, activityLogger)
                } else {
                    Timber.d("BootCompleteReceiver: Launch on boot is disabled, skipping immediate launch")
                }
            }
        }
    }

    /**
     * Launches all configured apps from the AppDataStore.
     * Logs each launch attempt for monitoring purposes.
     */
    private suspend fun launchConfiguredApps(
        context: Context,
        activityLogger: AppActivityLogger,
    ) {
        try {
            val dataStore = AppDataStore.store(context)
            val configuredApps: List<AppInfo> = dataStore.data.first()

            if (configuredApps.isEmpty()) {
                Timber.w("BootCompleteReceiver: No apps configured to launch")
                return
            }

            Timber.d("BootCompleteReceiver: Found ${configuredApps.size} apps to launch")

            configuredApps.forEach { appInfo ->
                Timber.d("BootCompleteReceiver: Launching app: ${appInfo.appName} (${appInfo.packageName})")
                val launched = AppLauncher.launchApp(context, appInfo.packageName)

                // Log the boot launch activity
                val logEntry =
                    AppActivityLog(
                        timestamp = Date().time,
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        action = if (launched) "BOOT_LAUNCH_SUCCESS" else "BOOT_LAUNCH_FAILED",
                        message = if (launched) "App launched on boot" else "Failed to launch app on boot",
                    )
                activityLogger.logActivity(logEntry)

                if (launched) {
                    Timber.d("BootCompleteReceiver: Successfully launched ${appInfo.appName}")
                } else {
                    Timber.w("BootCompleteReceiver: Failed to launch ${appInfo.appName}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "BootCompleteReceiver: Error launching apps on boot")
        }
    }
}
