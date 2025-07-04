package dev.hossain.keepalive.ui.screen

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.hossain.keepalive.data.PermissionType
import timber.log.Timber

/**
 * ViewModel responsible for managing and checking app permissions required for core functionality.
 * Tracks granted and remaining permissions, and exposes permission status for UI.
 */
class MainViewModel : ViewModel() {
    val allPermissionsGranted = MutableLiveData(false)

    /**
     * Total number of permissions required by the app.
     * On [android.os.Build.VERSION_CODES.TIRAMISU] and above, it includes [android.Manifest.permission.POST_NOTIFICATIONS] permission.
     */
    val totalPermissionRequired =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionType.entries.size
        } else {
            PermissionType.entries.size - 1
        }
    val totalApprovedPermissions = MutableLiveData(0)

    /**
     * Permissions that are required but not granted yet.
     * Having empty set after [checkAllPermissions] means all required permissions are granted.
     * @see checkAllPermissions
     */
    val requiredPermissionRemaining = mutableSetOf<PermissionType>()
    val grantedPermissions = mutableSetOf<PermissionType>()

    /**
     * List of permissions required by the app, based on Android version.
     */
    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.PACKAGE_USAGE_STATS)
        } else {
            arrayOf(Manifest.permission.PACKAGE_USAGE_STATS)
        }

    /**
     * Checks and updates the status of all required permissions.
     * Updates LiveData and permission sets accordingly.
     */
    fun checkAllPermissions(context: Context) {
        val hasUsageStatsPermission = hasUsageStatsPermission(context)
        val isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        val hasOverlayPermission = hasOverlayPermission(context)

        requiredPermissionRemaining.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotificationPermission =
                isPermissionGranted(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
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

        Timber.Forest.d("requiredPermissionRemaining=$requiredPermissionRemaining")
        totalApprovedPermissions.value = grantedPermissions.size
        allPermissionsGranted.value = requiredPermissionRemaining.isEmpty()
    }

    /**
     * Returns true if battery optimizations are ignored for the app.
     */
    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Returns true if the app has usage stats permission.
     */
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns true if the app has overlay (draw over other apps) permission.
     */
    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Returns true if all specified permissions are granted.
     */
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
            Timber.Forest.d("permission=$permission | status=$checkSelfPermission")
            checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns true if the specified permission is granted.
     */
    private fun isPermissionGranted(
        context: Context,
        permission: String,
    ): Boolean {
        val checkSelfPermission = ContextCompat.checkSelfPermission(context, permission)
        Timber.Forest.d("permission=$permission | status=$checkSelfPermission")
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }
}
