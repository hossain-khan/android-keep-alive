package dev.hossain.keepalive.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dev.hossain.keepalive.MainActivity
import dev.hossain.keepalive.R
import dev.hossain.keepalive.broadcast.NotificationActionReceiver
import dev.hossain.keepalive.data.NotificationVerbosity
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class for creating and managing notifications related to the foreground service.
 *
 * This class is responsible for:
 * - Creating the notification channel required for displaying notifications on Android Oreo (API 26) and above.
 * - Building the persistent notification that is displayed when the [WatchdogService] is running in the foreground.
 * - Building notifications for app restart events.
 * - Adding action buttons for quick interactions (Check Now, Pause/Resume Monitoring).
 *
 * The foreground service notification is essential to ensure the [WatchdogService] remains active
 * and is not prematurely killed by the Android system.
 *
 * @param context The application [Context], used to access system services like [NotificationManager]
 *                and to create [Intent]s for notification actions.
 */
class NotificationHelper(private val context: Context) {
    companion object {
        /**
         * Unique ID for the notification channel used by the WatchdogService.
         * This ID is used when creating the channel and when building notifications to associate them with this channel.
         */
        const val CHANNEL_ID = "WatchdogServiceChannel"

        /**
         * Unique ID for the notification channel used for app restart events.
         */
        const val APP_RESTART_CHANNEL_ID = "AppRestartChannel"

        /**
         * Base notification ID for app restart notifications.
         * Each app will have a unique notification ID based on this plus a hash of the package name.
         */
        const val APP_RESTART_NOTIFICATION_ID_BASE = 1000

        /**
         * Request code for the "Check Now" action.
         */
        const val REQUEST_CODE_CHECK_NOW = 100

        /**
         * Request code for the "Pause Monitoring" action.
         */
        const val REQUEST_CODE_PAUSE_MONITORING = 101

        /**
         * Request code for the "Resume Monitoring" action.
         */
        const val REQUEST_CODE_RESUME_MONITORING = 102
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Creates the notification channels for the WatchdogService.
     *
     * This method should be called before trying to display any notifications on Android Oreo (API 26) or higher.
     * It creates notification channels for:
     * - Foreground service notifications (low importance)
     * - App restart event notifications (default importance)
     *
     * If the channels already exist, this operation has no effect.
     * For devices below API 26, this method does nothing as notification channels are not required.
     */
    fun createNotificationChannel() {
        Timber.d("createNotificationChannel() called")

        // NotificationChannel is only available on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main watchdog service channel
            val watchdogChannel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name_watchdog_service),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_channel_description_watchdog)
                }

            // App restart events channel
            val appRestartChannel =
                NotificationChannel(
                    APP_RESTART_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name_app_restart),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.notification_channel_description_app_restart)
                }

            notificationManager.createNotificationChannel(watchdogChannel)
            notificationManager.createNotificationChannel(appRestartChannel)
        }
    }

    /**
     * Builds the persistent [Notification] for the foreground [WatchdogService].
     *
     * The notification informs the user that the app watchdog is active.
     * It includes:
     * - A title and content text.
     * - A small icon.
     * - A [PendingIntent] that opens [MainActivity] when the notification is tapped.
     * - Low priority and is set to ongoing to make it persistent.
     * - Optional last check time display based on verbosity.
     * - Action buttons for "Check Now" and "Pause/Resume Monitoring".
     *
     * @param lastCheckTime The timestamp of the last successful check in milliseconds, or 0 if no check yet.
     * @param isPaused Whether monitoring is currently paused.
     * @param verbosity The notification verbosity level.
     * @param monitoredAppsCount The number of apps being monitored.
     * @return The configured [Notification] object.
     */
    fun buildNotification(
        lastCheckTime: Long = 0L,
        isPaused: Boolean = false,
        verbosity: NotificationVerbosity = NotificationVerbosity.NORMAL,
        monitoredAppsCount: Int = 0,
    ): Notification {
        Timber.d("buildNotification() called with lastCheckTime=$lastCheckTime, isPaused=$isPaused, verbosity=$verbosity")

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(context, 0, notificationIntent, pendingIntentFlags)

        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_radar_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

        // Set title and content based on paused state
        if (isPaused) {
            builder.setContentTitle(context.getString(R.string.notification_title_monitoring_paused))
            builder.setContentText(context.getString(R.string.notification_content_monitoring_paused))
        } else {
            builder.setContentTitle(context.getString(R.string.notification_title_app_watchdog))

            // Build content text based on verbosity
            val contentText = buildContentText(lastCheckTime, verbosity, monitoredAppsCount)
            builder.setContentText(contentText)

            // Add detailed info in expanded view for verbose mode
            if (verbosity == NotificationVerbosity.VERBOSE && lastCheckTime > 0) {
                val expandedText = buildExpandedText(lastCheckTime, monitoredAppsCount)
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            }
        }

        // Add action buttons based on verbosity (not for QUIET mode)
        if (verbosity != NotificationVerbosity.QUIET) {
            addActionButtons(builder, isPaused)
        }

        return builder.build()
    }

    /**
     * Builds the content text for the notification based on verbosity level.
     */
    private fun buildContentText(
        lastCheckTime: Long,
        verbosity: NotificationVerbosity,
        monitoredAppsCount: Int,
    ): String {
        return when (verbosity) {
            NotificationVerbosity.QUIET -> {
                context.getString(R.string.notification_content_monitoring_apps)
            }
            NotificationVerbosity.NORMAL -> {
                if (monitoredAppsCount > 0) {
                    context.getString(R.string.notification_content_monitoring_count, monitoredAppsCount)
                } else {
                    context.getString(R.string.notification_content_monitoring_apps)
                }
            }
            NotificationVerbosity.VERBOSE -> {
                if (lastCheckTime > 0) {
                    val formattedTime = formatTime(lastCheckTime)
                    context.getString(R.string.notification_content_last_check, formattedTime)
                } else {
                    context.getString(R.string.notification_content_monitoring_apps)
                }
            }
        }
    }

    /**
     * Builds the expanded text for verbose notifications.
     */
    private fun buildExpandedText(
        lastCheckTime: Long,
        monitoredAppsCount: Int,
    ): String {
        val formattedTime = formatTime(lastCheckTime)
        return context.getString(
            R.string.notification_content_verbose_expanded,
            monitoredAppsCount,
            formattedTime,
        )
    }

    /**
     * Adds action buttons to the notification builder.
     */
    private fun addActionButtons(
        builder: NotificationCompat.Builder,
        isPaused: Boolean,
    ) {
        // Check Now action (only when not paused)
        if (!isPaused) {
            val checkNowIntent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_CHECK_NOW
                }
            val checkNowPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_CHECK_NOW,
                    checkNowIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            builder.addAction(
                R.drawable.baseline_radar_24,
                context.getString(R.string.notification_action_check_now),
                checkNowPendingIntent,
            )
        }

        // Pause/Resume Monitoring action
        if (isPaused) {
            val resumeIntent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_RESUME_MONITORING
                }
            val resumePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_RESUME_MONITORING,
                    resumeIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            builder.addAction(
                R.drawable.baseline_radar_24,
                context.getString(R.string.notification_action_resume),
                resumePendingIntent,
            )
        } else {
            val pauseIntent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_PAUSE_MONITORING
                }
            val pausePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PAUSE_MONITORING,
                    pauseIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            builder.addAction(
                R.drawable.baseline_radar_24,
                context.getString(R.string.notification_action_pause),
                pausePendingIntent,
            )
        }
    }

    /**
     * Shows a notification when an app is restarted.
     *
     * This notification is only shown when verbosity is NORMAL or VERBOSE.
     *
     * @param appName The name of the app that was restarted.
     * @param packageName The package name of the app.
     * @param verbosity The notification verbosity level.
     */
    fun showAppRestartNotification(
        appName: String,
        packageName: String,
        verbosity: NotificationVerbosity,
    ) {
        // Only show restart notifications for NORMAL and VERBOSE modes
        if (verbosity == NotificationVerbosity.QUIET) {
            Timber.d("Skipping app restart notification for $appName - verbosity is QUIET")
            return
        }

        Timber.d("Showing app restart notification for $appName")

        val notificationIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                packageName.hashCode(),
                notificationIntent,
                pendingIntentFlags,
            )

        val notification =
            NotificationCompat.Builder(context, APP_RESTART_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title_app_restarted))
                .setContentText(context.getString(R.string.notification_content_app_restarted, appName))
                .setSmallIcon(R.drawable.baseline_radar_24)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

        // Use a unique notification ID based on package name
        val notificationId = APP_RESTART_NOTIFICATION_ID_BASE + (packageName.hashCode() and 0x7FFFFFFF) % 1000
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Updates the foreground service notification.
     *
     * @param notificationId The notification ID used by the foreground service.
     * @param lastCheckTime The timestamp of the last successful check.
     * @param isPaused Whether monitoring is currently paused.
     * @param verbosity The notification verbosity level.
     * @param monitoredAppsCount The number of apps being monitored.
     */
    fun updateNotification(
        notificationId: Int,
        lastCheckTime: Long,
        isPaused: Boolean,
        verbosity: NotificationVerbosity,
        monitoredAppsCount: Int,
    ) {
        val notification = buildNotification(lastCheckTime, isPaused, verbosity, monitoredAppsCount)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Formats a timestamp into a human-readable time string.
     */
    private fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
