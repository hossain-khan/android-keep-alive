package dev.hossain.keepalive.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.hossain.keepalive.MainActivity
import dev.hossain.keepalive.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class WatchdogService : Service() {

    private val CHANNEL_ID = "WatchdogServiceChannel"
    private val NOTIFICATION_ID = 1
    private val TAG = "WatchdogService"

    companion object {
        const val TARGET_APP_PACKAGE_NAME =
            "com.example.targetapp" // Replace with your target app's package name
        const val CHECK_INTERVAL_MILLIS = 5_000L // Check every 5 seconds
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind: $intent")
        return null
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId"
        )
        createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            buildNotification()
        )

        // Periodically check if the target app is running


        // Log current time every 10 seconds
        GlobalScope.launch {
            while (true) {
                Log.d(TAG, "Current time: ${System.currentTimeMillis()} @ ${Date()}")
                delay(CHECK_INTERVAL_MILLIS)
            }
        }

        return START_STICKY // Restart the service if it's killed
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,

            "Watchdog Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        // Replace MainActivity with your app's main activity
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setContentTitle("Watchdog Service")
            .setContentText("Monitoring app status")
            .setSmallIcon(R.drawable.baseline_radar_24)
            .setContentIntent(pendingIntent)
            .build()
    }

    // ... (Other methods for checking app status and relaunching will be added later)
}