package dev.hossain.keepalive.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dev.hossain.keepalive.util.AppChecker
import dev.hossain.keepalive.util.AppConfig.OG_PIXEL_URL
import dev.hossain.keepalive.util.AppLauncher
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

        // Less time for debugging - 20 seconds
        //private const val CHECK_INTERVAL_MILLIS = 20_000L
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

                if (!AppChecker.isGooglePhotosRunning(this@WatchdogService)) {
                    Log.d(TAG, "Photos app is not running. Starting it now.")
                    AppLauncher.openGooglePhotos(this@WatchdogService)
                } else {
                    Log.d(TAG, "Photos app is already running.")
                    pingSender.sendPingToDevice()
                }

                delay(30_000L) // 30 seconds
                if (!AppChecker.isSyncthingRunning(this@WatchdogService)) {
                    Log.d(TAG, "Sync app is not running. Starting it now.")
                    AppLauncher.openSyncthing(this@WatchdogService)
                } else {
                    Log.d(TAG, "Sync app is already running.")
                    pingSender.sendPingToDevice()
                }

                delay(CHECK_INTERVAL_MILLIS)
            }
        }

        return START_STICKY // Restart the service if it's killed
    }


}