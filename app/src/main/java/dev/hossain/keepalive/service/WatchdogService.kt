package dev.hossain.keepalive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.ComponentName
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
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.util.Date


class WatchdogService : Service() {
    companion object {
        private const val CHANNEL_ID = "WatchdogServiceChannel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "WatchdogService"

        // https://play.google.com/store/apps/details?id=com.nutomic.syncthingandroid
        private const val SYNC_APP_PACKAGE_NAME = "com.nutomic.syncthingandroid"
        private const val SYNC_APP_LAUNCH_ACTIVITY =
            "com.nutomic.syncthingandroid.activities.FirstStartActivity"

        // https://play.google.com/store/apps/details?id=com.google.android.apps.photos
        private const val PHOTOS_APP_PACKAGE_NAME =
            "com.google.android.apps.photos"
        private const val PHOTOS_APP_LAUNCH_ACTIVITY =
            "com.google.android.apps.photos.home.HomeActivity"
        private const val CHECK_INTERVAL_MILLIS = 1800_000L // 30 minutes x2

        // Less time for debugging
        //private const val CHECK_INTERVAL_MILLIS = 20_000L // 30 minutes x2
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


        // Log current time every 10 seconds
        GlobalScope.launch {
            while (true) {
                Log.d(TAG, "Current time: ${System.currentTimeMillis()} @ ${Date()}")
                delay(CHECK_INTERVAL_MILLIS)

                if (!AppChecker.isAppRunning(this@WatchdogService, PHOTOS_APP_PACKAGE_NAME)) {
                    Log.d(TAG, "Photos app is not running. Starting it now.")
                    startApplication(PHOTOS_APP_PACKAGE_NAME, PHOTOS_APP_LAUNCH_ACTIVITY)
                } else {
                    Log.d(TAG, "Photos app is already running.")
                    sendHttpPing()
                }

                delay(30_000L) // 30 seconds
                if (!AppChecker.isAppRunning(this@WatchdogService, SYNC_APP_PACKAGE_NAME)) {
                    Log.d(TAG, "Sync app is not running. Starting it now.")
                    startApplication(SYNC_APP_PACKAGE_NAME, SYNC_APP_LAUNCH_ACTIVITY)
                } else {
                    Log.d(TAG, "Sync app is already running.")
                    sendHttpPing()
                }

                delay(CHECK_INTERVAL_MILLIS)
            }
        }

        return START_STICKY // Restart the service if it's killed
    }

    private fun startApplication(packageName: String, activityName: String) {

        // Start the activity
        val launchIntent = Intent()
        launchIntent.setAction(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.setComponent(
            ComponentName(packageName, activityName)
        )
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Log.e(TAG, "Unable to find activity: $launchIntent", e)
        }
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


    // Send HTTP request to https://hc-ping.com/357a4e95-a7b3-4cd0-9506-4168fd9f1794
    private fun sendHttpPing() {
        val url = "https://hc-ping.com/357a4e95-a7b3-4cd0-9506-4168fd9f1794"
        val client = OkHttpClient()

        // Get app current version
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName

        // Add user agent with app name, version, and device info
        val userAgent = "KA/${versionName} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT} ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL})"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Unexpected code $response", IOException("Unexpected code $response"))
            } else {
                Log.d(TAG, "sendHttpPing: Response: ${response.body!!.string()}")
            }
        }
    }
}