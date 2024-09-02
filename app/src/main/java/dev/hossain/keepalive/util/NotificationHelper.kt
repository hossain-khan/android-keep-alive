package dev.hossain.keepalive.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.hossain.keepalive.MainActivity
import dev.hossain.keepalive.R

/**
 * Helper class to create notification channel and build notification.
 */
class NotificationHelper(private val context: Context) {
    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID = "WatchdogServiceChannel"
    }

    fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel() called")

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Watchdog Service",
                NotificationManager.IMPORTANCE_LOW,
            )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    fun buildNotification(): Notification {
        Log.d(TAG, "buildNotification() called")

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Photo Auto Upload")
            .setContentText("📸 Monitoring photo upload apps.")
            .setSmallIcon(R.drawable.baseline_radar_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
