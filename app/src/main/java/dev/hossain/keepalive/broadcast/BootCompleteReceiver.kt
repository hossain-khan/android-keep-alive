package dev.hossain.keepalive.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        // Use device protected storage context
        val directBootContext: Context = context.createDeviceProtectedStorageContext()

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Start the WatchdogService
            Timber.d("BootCompleteReceiver onReceive: Starting WatchdogService")
            val serviceIntent = Intent(directBootContext, WatchdogService::class.java)
            directBootContext.startForegroundService(serviceIntent)
        }
    }
}
