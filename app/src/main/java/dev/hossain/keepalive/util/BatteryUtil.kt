package dev.hossain.keepalive.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Utility object for checking battery and charging status.
 */
object BatteryUtil {
    /**
     * Checks if the device is currently plugged into a charger.
     *
     * @param context The [Context] used to query battery status.
     * @return `true` if the device is plugged in (AC, USB, or Wireless), `false` otherwise.
     */
    fun isCharging(context: Context): Boolean {
        val batteryStatus: Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intentFilter ->
                context.registerReceiver(null, intentFilter)
            }

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        // Alternative check using plug type
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isPluggedIn =
            chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

        return isCharging || isPluggedIn
    }
}
