package dev.hossain.keepalive

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    val allPermissionsGranted = MutableLiveData(false)

    fun checkAllPermissions(context: Context) {
        val hasUsageStatsPermission = hasUsageStatsPermission(context)
        val isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        allPermissionsGranted.value = hasUsageStatsPermission && isBatteryOptimizationIgnored
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
