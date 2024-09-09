package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.hossain.keepalive.data.PermissionType
import timber.log.Timber

class MainViewModel : ViewModel() {
    val allPermissionsGranted = MutableLiveData(false)

    /**
     * Total number of permissions required by the app.
     * On [TIRAMISU] and above, it includes [POST_NOTIFICATIONS] permission.
     */
    val totalPermissionRequired = if (SDK_INT >= TIRAMISU) PermissionType.entries.size else PermissionType.entries.size - 1
    val totalApprovedPermissions = MutableLiveData(0)

    /**
     * Permissions that are required but not granted yet.
     * Having empty set after [checkAllPermissions] means all required permissions are granted.
     * @see checkAllPermissions
     */
    val requiredPermissionRemaining = mutableSetOf<PermissionType>()
    val grantedPermissions = mutableSetOf<PermissionType>()

    val requiredPermissions =
        if (SDK_INT >= TIRAMISU) {
            arrayOf(POST_NOTIFICATIONS, PACKAGE_USAGE_STATS)
        } else {
            arrayOf(PACKAGE_USAGE_STATS)
        }

    fun checkAllPermissions(context: Context) {
        val hasUsageStatsPermission = hasUsageStatsPermission(context)
        val isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        val hasOverlayPermission = hasOverlayPermission(context)

        requiredPermissionRemaining.clear()
        if (SDK_INT >= TIRAMISU) {
            val hasPostNotificationPermission = isPermissionGranted(context, POST_NOTIFICATIONS)
            if (!hasPostNotificationPermission) {
                requiredPermissionRemaining.add(PermissionType.PERMISSION_POST_NOTIFICATIONS)
            } else {
                grantedPermissions.add(PermissionType.PERMISSION_POST_NOTIFICATIONS)
            }
        }
        if (!isBatteryOptimizationIgnored) {
            requiredPermissionRemaining.add(PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS)
        } else {
            grantedPermissions.add(PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS)
        }
        if (!hasUsageStatsPermission) {
            requiredPermissionRemaining.add(PermissionType.PERMISSION_PACKAGE_USAGE_STATS)
        } else {
            grantedPermissions.add(PermissionType.PERMISSION_PACKAGE_USAGE_STATS)
        }
        if (!hasOverlayPermission) {
            requiredPermissionRemaining.add(PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY)
        } else {
            grantedPermissions.add(PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY)
        }

        Timber.d("requiredPermissionRemaining=$requiredPermissionRemaining")
        totalApprovedPermissions.value = grantedPermissions.size
        allPermissionsGranted.value = requiredPermissionRemaining.isEmpty()
    }

    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (SDK_INT >= Build.VERSION_CODES.Q) {
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

    private fun hasOverlayPermission(context: Context): Boolean {
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
            Timber.d("permission=$permission | status=$checkSelfPermission")
            checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isPermissionGranted(
        context: Context,
        permission: String,
    ): Boolean {
        val checkSelfPermission = ContextCompat.checkSelfPermission(context, permission)
        Timber.d("permission=$permission | status=$checkSelfPermission")
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }
}
