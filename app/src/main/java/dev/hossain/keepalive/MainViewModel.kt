package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    companion object {
        private const val TAG = "MainViewModel"
    }

    val allPermissionsGranted = MutableLiveData(false)

    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(POST_NOTIFICATIONS, PACKAGE_USAGE_STATS)
        } else {
            arrayOf(PACKAGE_USAGE_STATS)
        }

    fun checkAllPermissions(context: Context) {
        val hasUsageStatsPermission = hasUsageStatsPermission(context)
        val isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        val allOtherPermissionsGranted = arePermissionsGranted(context, requiredPermissions)
        val hasOverlayPermission = hasOverlayPermission(context)
        Log.d(
            TAG,
            "hasUsageStatsPermission=$hasUsageStatsPermission, " +
                "isBatteryOptimizationIgnored=$isBatteryOptimizationIgnored, " +
                "allOtherPermissionsGranted=$allOtherPermissionsGranted, " +
                "hasOverlayPermission=$hasOverlayPermission",
        )
        allPermissionsGranted.value = hasUsageStatsPermission &&
            isBatteryOptimizationIgnored &&
            // TODO: Check for other permissions
            hasOverlayPermission
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

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun arePermissionsGranted(
        context: Context,
        permissions: Array<String>,
    ): Boolean {
        return permissions.all { permission ->
            val checkSelfPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    permission,
                )
            Log.d(TAG, "permission=$permission | status=$checkSelfPermission")
            checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }
    }
}
