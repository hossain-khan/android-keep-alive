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
        Timber.d("BootCompleteReceiver received action: ${intent.action}")

        // Use device protected storage context
        val directBootContext: Context = context.createDeviceProtectedStorageContext()

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            // Start the WatchdogService
            Timber.d("BootCompleteReceiver onReceive: Starting WatchdogService for action: ${intent.action}")

            try {
                val serviceIntent = Intent(directBootContext, WatchdogService::class.java)
                directBootContext.startForegroundService(serviceIntent)
                Timber.d("BootCompleteReceiver: Successfully called startForegroundService() for WatchdogService")
            } catch (e: Exception) {
                Timber.e(e, "BootCompleteReceiver: Failed to start WatchdogService")
            }
        } else {
            Timber.w("BootCompleteReceiver: Received unexpected action: ${intent.action}")
        }
    }
}
