package dev.hossain.keepalive.service

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dev.hossain.keepalive.util.AppChecker
import dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_LAUNCH_ACTIVITY
import dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_PACKAGE_NAME
import dev.hossain.keepalive.util.AppConfig.SYNC_APP_LAUNCH_ACTIVITY
import dev.hossain.keepalive.util.AppConfig.SYNC_APP_PACKAGE_NAME
import dev.hossain.keepalive.util.AppConfig.ogPixelUrl
import dev.hossain.keepalive.util.HttpPingSender
import dev.hossain.keepalive.util.NotificationHelper
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Service that keeps an eye on the Google Photos and Sync app and restarts if it's killed.
 */
class WatchdogService : Service() {
    companion object {

        private const val NOTIFICATION_ID = 1
        private const val TAG = "WatchdogService"


        private const val CHECK_INTERVAL_MILLIS = 1800_000L // 30 minutes x2

        // Less time for debugging
        //private const val CHECK_INTERVAL_MILLIS = 20_000L // 30 minutes x2
    }

    private val pingSender = HttpPingSender(this)
    private val notificationHelper = NotificationHelper(this)


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
        notificationHelper.createNotificationChannel()

        startForeground(
            NOTIFICATION_ID,
            notificationHelper.buildNotification()
        )

        GlobalScope.launch {
            while (true) {
                Log.d(TAG, "Current time: ${System.currentTimeMillis()} @ ${Date()}")
                delay(CHECK_INTERVAL_MILLIS)

                // Send heart beat ping to URL for the device
                val watcherHeartbeat = ogPixelUrl

                if (!AppChecker.isAppRunning(this@WatchdogService, PHOTOS_APP_PACKAGE_NAME)) {
                    Log.d(TAG, "Photos app is not running. Starting it now.")
                    startApplication(PHOTOS_APP_PACKAGE_NAME, PHOTOS_APP_LAUNCH_ACTIVITY)
                } else {
                    Log.d(TAG, "Photos app is already running.")
                    pingSender.sendHttpPing(watcherHeartbeat)
                }

                delay(30_000L) // 30 seconds
                if (!AppChecker.isAppRunning(this@WatchdogService, SYNC_APP_PACKAGE_NAME)) {
                    Log.d(TAG, "Sync app is not running. Starting it now.")
                    startApplication(SYNC_APP_PACKAGE_NAME, SYNC_APP_LAUNCH_ACTIVITY)
                } else {
                    Log.d(TAG, "Sync app is already running.")
                    pingSender.sendHttpPing(watcherHeartbeat)
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
}