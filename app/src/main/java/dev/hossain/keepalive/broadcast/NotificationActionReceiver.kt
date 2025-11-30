package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Receiver for handling notification action button clicks.
 *
 * Handles the following actions:
 * - "Check Now": Triggers an immediate app check.
 * - "Pause Monitoring": Pauses the monitoring service.
 * - "Resume Monitoring": Resumes the monitoring service.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CHECK_NOW = "dev.hossain.keepalive.ACTION_CHECK_NOW"
        const val ACTION_PAUSE_MONITORING = "dev.hossain.keepalive.ACTION_PAUSE_MONITORING"
        const val ACTION_RESUME_MONITORING = "dev.hossain.keepalive.ACTION_RESUME_MONITORING"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.d("NotificationActionReceiver onReceive: action=${intent.action}")

        when (intent.action) {
            ACTION_CHECK_NOW -> handleCheckNow(context)
            ACTION_PAUSE_MONITORING -> handlePauseMonitoring(context)
            ACTION_RESUME_MONITORING -> handleResumeMonitoring(context)
        }
    }

    /**
     * Handles the "Check Now" action by restarting the WatchdogService
     * which will trigger an immediate check.
     */
    private fun handleCheckNow(context: Context) {
        Timber.d("Handling Check Now action")

        // Restart the service to trigger an immediate check
        val serviceIntent =
            Intent(context, WatchdogService::class.java).apply {
                action = WatchdogService.ACTION_CHECK_NOW
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * Handles the "Pause Monitoring" action by saving the paused state
     * and updating the notification.
     */
    private fun handlePauseMonitoring(context: Context) {
        Timber.d("Handling Pause Monitoring action")

        receiverScope.launch {
            val settingsRepository = SettingsRepository(context)
            settingsRepository.saveIsMonitoringPaused(true)

            // Update the notification to reflect the paused state
            updateNotification(context, settingsRepository, isPaused = true)
        }
    }

    /**
     * Handles the "Resume Monitoring" action by saving the resumed state
     * and updating the notification.
     */
    private fun handleResumeMonitoring(context: Context) {
        Timber.d("Handling Resume Monitoring action")

        receiverScope.launch {
            val settingsRepository = SettingsRepository(context)
            settingsRepository.saveIsMonitoringPaused(false)

            // Update the notification to reflect the resumed state
            updateNotification(context, settingsRepository, isPaused = false)
        }
    }

    /**
     * Updates the foreground service notification with the current state.
     */
    private suspend fun updateNotification(
        context: Context,
        settingsRepository: SettingsRepository,
        isPaused: Boolean,
    ) {
        val notificationHelper = NotificationHelper(context)
        val verbosity = settingsRepository.notificationVerbosityFlow.first()
        val lastCheckTime = settingsRepository.lastCheckTimeFlow.first()

        notificationHelper.updateNotification(
            notificationId = WatchdogService.NOTIFICATION_ID,
            lastCheckTime = lastCheckTime,
            isPaused = isPaused,
            verbosity = verbosity,
            // Will be updated by the service
            monitoredAppsCount = 0,
        )
    }
}
