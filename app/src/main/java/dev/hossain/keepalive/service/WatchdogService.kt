package dev.hossain.keepalive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.hossain.keepalive.MainActivity
import dev.hossain.keepalive.R
import dev.hossain.keepalive.util.AppChecker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

class WatchdogService : Service() {
    companion object {
        private const val CHANNEL_ID = "WatchdogServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "WatchdogService"
        // https://play.google.com/store/apps/details?id=com.google.android.apps.photos
        const val PHOTOS_APP_PACKAGE_NAME =
            "com.google.android.apps.photos" // Replace with your target app's package name
        const val CHECK_INTERVAL_MILLIS = 10_000L // Check every 5 seconds
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

                if(!AppChecker.isAppRunning(this@WatchdogService, PHOTOS_APP_PACKAGE_NAME)) {
                    Log.d(TAG, "Photos app is not running. Starting it now.")
                    startExplicitApp(PHOTOS_APP_PACKAGE_NAME)
                } else {
                    Log.d(TAG, "Photos app is already running.")
                }
            }
        }

        return START_STICKY // Restart the service if it's killed
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel() called")

        val channel = NotificationChannel(
            CHANNEL_ID,

            "Watchdog Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        Log.d(TAG, "buildNotification() called")

        val notificationIntent =
            Intent(/* packageContext = */ this, /* cls = */ MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(/* context = */ this, /* requestCode = */
            0, /* intent = */
            notificationIntent, /* flags = */
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Watchdog Service")
            .setContentText("Monitoring app status")
            .setSmallIcon(R.drawable.baseline_radar_24)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Set priority
            .setOngoing(true) // Make the notification sticky
            .build()
    }

    private fun startPhotosApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (launchIntent != null) {
            Log.i(TAG, "Starting Photos app with intent: $launchIntent")
            startActivity(launchIntent)
        } else {
            Log.e(TAG, "Unable to find package: $packageName")
        }
    }

    private fun startExplicitApp(packageName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for starting from a service
        }
        startActivity(intent)
    }
}