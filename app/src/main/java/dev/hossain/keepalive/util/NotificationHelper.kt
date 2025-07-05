package dev.hossain.keepalive.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.hossain.keepalive.MainActivity
import dev.hossain.keepalive.R
import timber.log.Timber

/**
 * Helper class for creating and managing notifications related to the foreground service.
 *
 * This class is responsible for:
 * - Creating the notification channel required for displaying notifications on Android Oreo (API 26) and above.
 * - Building the persistent notification that is displayed when the [WatchdogService] is running in the foreground.
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
        private const val CHANNEL_ID = "WatchdogServiceChannel"
    }

    /**
     * Creates the notification channel for the WatchdogService.
     *
     * This method should be called before trying to display any notifications on Android Oreo (API 26) or higher.
     * It creates a [NotificationChannel] with a default importance and registers it with the [NotificationManager].
     * If the channel already exists, this operation has no effect.
     */
    fun createNotificationChannel() {
        Timber.d("createNotificationChannel() called")

        // NotificationChannel is only available on API 26+
        // No need for an explicit Build.VERSION.SDK_INT check here, as NotificationChannel constructor handles it.
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name_watchdog_service),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
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
     *
     * @return The configured [Notification] object.
     */
    fun buildNotification(): Notification {
        Timber.d("buildNotification() called")

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntentFlags =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent =
            PendingIntent.getActivity(context, 0, notificationIntent, pendingIntentFlags)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title_app_watchdog))
            .setContentText(context.getString(R.string.notification_content_monitoring_apps))
            .setSmallIcon(R.drawable.baseline_radar_24)
            .setContentIntent(pendingIntent)
            // Low priority for ongoing background service notification
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Makes the notification persistent
            .setOngoing(true)
            .build()
    }
}
