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
 * ViewModel responsible for managing and checking app permissions essential for the app's core functionality.
 *
 * This ViewModel:
 * - Tracks the overall status of all required permissions via [allPermissionsGranted].
 * - Calculates the [totalPermissionRequired] based on the Android version.
 * - Keeps a count of [totalApprovedPermissions].
 * - Maintains sets of [requiredPermissionRemaining] and [grantedPermissions].
 * - Provides a list of [requiredPermissions] (manifest permissions) based on the Android version.
 * - Offers methods to [checkAllPermissions] and verify individual or groups of permissions.
 */
class MainViewModel : ViewModel() {
    /** LiveData indicating whether all necessary permissions have been granted. */
    val allPermissionsGranted = MutableLiveData(false)

    /**
     * Total number of distinct permission types required by the app.
     * This count varies based on the Android SDK version, for example,
     * [android.Manifest.permission.POST_NOTIFICATIONS] is only required on
     * [android.os.Build.VERSION_CODES.TIRAMISU] and above.
     */
    val totalPermissionRequired: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionType.entries.size
        } else {
            // Exclude PERMISSION_POST_NOTIFICATIONS if below TIRAMISU
            PermissionType.entries.count { it != PermissionType.PERMISSION_POST_NOTIFICATIONS }
        }

    /** LiveData holding the count of currently approved (granted) permissions. */
    val totalApprovedPermissions = MutableLiveData(0)

    /**
     * A mutable set of [PermissionType] that are currently required by the app but have not yet been granted.
     * This set is populated by [checkAllPermissions]. If this set is empty, it implies all permissions are granted.
     */
    val requiredPermissionRemaining = mutableSetOf<PermissionType>()

    /** A mutable set of [PermissionType] that have been successfully granted. */
    val grantedPermissions = mutableSetOf<PermissionType>()

    /**
     * Array of Android Manifest permission strings required by the app.
     * The specific permissions in this array depend on the Android SDK version.
     * For example, [android.Manifest.permission.POST_NOTIFICATIONS] is included only on
     * [android.os.Build.VERSION_CODES.TIRAMISU] and higher.
     */
    val requiredPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.PACKAGE_USAGE_STATS)
        } else {
            arrayOf(Manifest.permission.PACKAGE_USAGE_STATS)
        }

    /**
     * Checks the status of all permissions required by the application and updates the ViewModel's state.
     *
     * This function performs the following actions:
     * 1. Clears the current [requiredPermissionRemaining] and [grantedPermissions] sets.
     * 2. Checks for [Manifest.permission.POST_NOTIFICATIONS] if on Android Tiramisu (API 33) or higher.
     *    Adds to [requiredPermissionRemaining] if not granted, or [grantedPermissions] if granted.
     * 3. Checks if battery optimizations are ignored using [isBatteryOptimizationIgnored].
     *    Adds [PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS] to appropriate set.
     * 4. Checks for usage stats permission using [hasUsageStatsPermission].
     *    Adds [PermissionType.PERMISSION_PACKAGE_USAGE_STATS] to appropriate set.
     * 5. Checks for overlay permission using [hasOverlayPermission].
     *    Adds [PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY] to appropriate set.
     * 6. Updates [totalApprovedPermissions] LiveData with the size of the [grantedPermissions] set.
     * 7. Updates [allPermissionsGranted] LiveData based on whether [requiredPermissionRemaining] is empty.
     *
     * @param context The application context, used to check permission statuses.
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

        Timber.d("requiredPermissionRemaining=$requiredPermissionRemaining")
        totalApprovedPermissions.value = grantedPermissions.size
        allPermissionsGranted.value = requiredPermissionRemaining.isEmpty()
    }

    /**
     * Checks if the application is currently exempt from battery optimizations.
     *
     * @param context The application context.
     * @return `true` if battery optimizations are ignored for the app, `false` otherwise.
     */
    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Checks if the application has been granted permission to access package usage statistics.
     * This permission ([android.Manifest.permission.PACKAGE_USAGE_STATS]) is a special permission
     * that requires the user to grant it through system settings.
     *
     * @param context The application context.
     * @return `true` if the app has usage stats permission, `false` otherwise.
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
     * Checks if the application has permission to draw overlays on top of other apps.
     * This permission ([android.Manifest.permission.SYSTEM_ALERT_WINDOW]) is a special permission
     * that requires the user to grant it through system settings.
     *
     * @param context The application context.
     * @return `true` if the app can draw overlays, `false` otherwise.
     */
    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Checks if all permissions in the provided array are currently granted.
     *
     * @param context The application context.
     * @param permissions An array of Android Manifest permission strings to check.
     * @return `true` if all specified permissions are granted, `false` otherwise.
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
            Timber.d("permission=$permission | status=$checkSelfPermission")
            checkSelfPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if a specific Android Manifest permission is currently granted.
     *
     * @param context The application context.
     * @param permission The Android Manifest permission string to check.
     * @return `true` if the specified permission is granted, `false` otherwise.
     */
    private fun isPermissionGranted(
        context: Context,
        permission: String,
    ): Boolean {
        val checkSelfPermission = ContextCompat.checkSelfPermission(context, permission)
        Timber.d("permission=$permission | status=$checkSelfPermission")
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED
    }
}
