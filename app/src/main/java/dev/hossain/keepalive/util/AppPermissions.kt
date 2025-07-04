package dev.hossain.keepalive.util

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.data.PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_PACKAGE_USAGE_STATS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_POST_NOTIFICATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY
import timber.log.Timber

/**
 * Utility object for handling requests for various Android permissions required by the application.
 *
 * This object centralizes the logic for requesting permissions such as:
 * - Battery optimization exclusion
 * - Overlay permission (draw over other apps)
 * - Usage stats access
 * - Notification listener access (though this seems unused currently based on `requestPermission` logic)
 * - Post notifications (for Android Tiramisu and above)
 *
 * It provides a generic [requestPermission] method that delegates to specific handlers based on [PermissionType].
 */
object AppPermissions {
    /**
     * Requests the user to exclude the application from battery optimizations.
     *
     * This navigates the user to the system settings screen where they can disable battery optimization
     * for this app. A [Toast] message is shown to guide the user.
     *
     * @param context The [Context] used to show the Toast and start the settings activity.
     */
    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizationExclusion(context: Context) {
        Toast.makeText(
            context,
            "Please exclude this app from battery optimization.",
            Toast.LENGTH_SHORT,
        ).show()
        val intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        context.startActivity(intent)
    }

    /**
     * Requests the overlay permission (to draw over other apps).
     *
     * This navigates the user to the system settings screen where they can grant the
     * "Display over other apps" permission. This is necessary for features that require
     * showing UI elements while the app is not in the foreground (though the app's current
     * usage might be for starting activities from background service).
     *
     * Background Activity Launch Restrictions (Android 10+):
     * Starting from Android 10 (API level 29), apps have restrictions on launching activities
     * from the background to improve user experience. Overlay permission can sometimes be
     * a prerequisite or related to scenarios where apps might attempt such launches.
     *
     * @param context The [Context] (though unused directly, kept for consistency or future use).
     * @param activityResultLauncher The [ActivityResultLauncher] used to launch the system settings intent
     *                               and handle the result.
     */
    fun requestOverlayPermission(
        context: Context,
        activityResultLauncher: ActivityResultLauncher<Intent>,
    ) {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            )
        activityResultLauncher.launch(intent)
    }

    /**
     * Requests the "Usage Access" permission.
     *
     * This navigates the user to the system settings screen where they can grant permission
     * for the app to access information about app usage (e.g., how long apps are used,
     * last time used). This is crucial for the [RecentAppChecker] functionality.
     *
     * @param context The [Context] used to start the settings activity.
     */
    fun requestUsageStatsPermission(context: Context) {
        Timber.d("requestUsageStatsPermission: Requesting usage stats permission")
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * Requests the "Notification Listener" permission.
     *
     * This navigates the user to the system settings screen where they can grant permission
     * for the app to read notifications.
     *
     * Note: This method is currently defined but not actively called by the main [requestPermission]
     * switch statement for any [PermissionType]. It might be legacy or for future use.
     *
     * @param context The [Context] used to start the settings activity.
     */
    fun requestNotificationAccess(context: Context) {
        Timber.d("requestNotificationAccess: Requesting notification access permission")
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * Requests the [POST_NOTIFICATIONS] permission using an [ActivityResultLauncher].
     * This is specifically for Android 13 (Tiramisu, API 33) and above, where posting
     * notifications becomes a runtime permission.
     *
     * @param requestPermissionLauncher The [ActivityResultLauncher] for `Array<String>` (multiple permissions)
     *                                  used to request the notification permission.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPostNotificationPermission(requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        Timber.d("requestPostNotificationPermission: Requesting post notification permission")
        requestPermissionLauncher.launch(arrayOf(POST_NOTIFICATIONS))
    }

    /**
     * Central dispatcher for requesting a specific [PermissionType].
     *
     * This function determines the type of permission requested and calls the appropriate
     * specific request method (e.g., [requestPostNotificationPermission], [requestUsageStatsPermission]).
     * It handles API level checks implicitly through the called methods or explicitly where needed.
     *
     * @param context The application [Context].
     * @param activityResultLauncher An [ActivityResultLauncher] for `Intent`, used for permissions
     *                               that require navigating to system settings (e.g., overlay, battery optimization).
     *                               Can be null if the permission type doesn't require it.
     * @param requestPermissionLauncher An [ActivityResultLauncher] for `Array<String>`, used for
     *                                  standard runtime permissions (e.g., post notifications).
     *                                  Can be null if the permission type doesn't require it.
     * @param permissionType The [PermissionType] enum indicating which permission to request.
     */
    @SuppressLint("NewApi") // Suppresses NewApi lint for VERSION_CODES.TIRAMISU check handled by target function
    fun requestPermission(
        context: Context,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
        permissionType: PermissionType,
    ) {
        Timber.d("requestPermission: for $permissionType")
        when (permissionType) {
            PERMISSION_POST_NOTIFICATIONS -> {
                // Request for notification permission
                AppPermissions.requestPostNotificationPermission(requestPermissionLauncher!!)
            }

            PERMISSION_PACKAGE_USAGE_STATS -> {
                // Request for usage stats permission
                AppPermissions.requestUsageStatsPermission(context)
            }

            PERMISSION_SYSTEM_APPLICATION_OVERLAY -> {
                // Request for overlay permission
                activityResultLauncher?.let {
                    AppPermissions.requestOverlayPermission(context, it)
                }
            }

            PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS -> {
                // Request for battery optimization exclusion
                AppPermissions.requestBatteryOptimizationExclusion(context)
            }

            else -> {
                // Do nothing
            }
        }
    }
}
