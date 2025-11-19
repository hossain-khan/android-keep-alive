package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.hossain.keepalive.service.WatchdogService
import timber.log.Timber

/**
 * Receiver to start the WatchdogService on boot complete.
 */
class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.d("BootCompleteReceiver onReceive() called with: context = $context, intent = $intent")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Start the WatchdogService
            Timber.d("BootCompleteReceiver onReceive: Starting WatchdogService")
            val serviceIntent = Intent(context, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
