package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
     * This number needs to be dynamic based on the permissions required.
     * We could leverage the number of ENUMS in [PermissionType] to calculate this.
     * Some cleanup required later.
     */
    val totalPermissionRequired = 4
    val totalApprovedPermissions = MutableLiveData(0)

    /**
     * Permissions that are required but not granted yet.
     * Having empty set after [checkAllPermissions] means all required permissions are granted.
     * @see checkAllPermissions
     */
    val requiredPermissionRemaining = mutableSetOf<PermissionType>()
    val grantedPermissions = mutableSetOf<PermissionType>()

    val requiredPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(POST_NOTIFICATIONS, PACKAGE_USAGE_STATS)
        } else {
            arrayOf(PACKAGE_USAGE_STATS)
        }

    fun checkAllPermissions(context: Context) {
        val hasUsageStatsPermission = hasUsageStatsPermission(context)
        val isBatteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        val hasPackageUsageStatsPermission = isPermissionGranted(context, PACKAGE_USAGE_STATS)
        val hasOverlayPermission = hasOverlayPermission(context)

        requiredPermissionRemaining.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        // TODO what is the difference between this and `hasUsageStatsPermission`?
//        if(!hasPackageUsageStatsPermission) {
//            requiredPermissionRemaining.add(PermissionType.PERMISSION_PACKAGE_USAGE_STATS)
//        }

        Timber.d("requiredPermissionRemaining=$requiredPermissionRemaining")
        totalApprovedPermissions.value = grantedPermissions.size
        allPermissionsGranted.value = requiredPermissionRemaining.isEmpty()
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
            Timber.d("permission=$permission | status=$checkSelfPermission")
            checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isPermissionGranted(
        context: Context,
        permission: String,
    ): Boolean {
        val checkSelfPermission = ContextCompat.checkSelfPermission(context, permission)
        Timber.d("permission=$permission | status=$checkSelfPermission")
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }
}
