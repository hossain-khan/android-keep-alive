package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.hossain.keepalive.service.WatchdogService

/**
 * Receiver to start the WatchdogService on boot complete.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the WatchdogService
            val serviceIntent = Intent(context, WatchdogService::class.java)
            context?.startForegroundService(serviceIntent)
        }
    }
}