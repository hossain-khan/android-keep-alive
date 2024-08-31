package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.hossain.keepalive.service.WatchdogService

/**
 * Receiver to start the WatchdogService on boot complete.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    private val TAG = "BootCompleteReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() called with: context = $context, intent = $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Start the WatchdogService
            Log.d(TAG, "onReceive: Starting WatchdogService")
            val serviceIntent = Intent(context, WatchdogService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}