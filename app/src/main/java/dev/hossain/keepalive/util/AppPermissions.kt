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

object AppPermissions {
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
     * When your app's WindowStopped is set to true, it means that your app's activity has been stopped,
     * which typically occurs when the app is no longer visible to the user. Starting a new activity
     * when your app's WindowStopped is true is restricted on newer versions of Android due to the
     * background activity launch restrictions.
     *
     * Understanding the Restriction
     * Starting from Android 10 (API level 29), apps are restricted from launching activities from
     * the background to improve the user experience and reduce unexpected interruptions.
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

    fun requestUsageStatsPermission(context: Context) {
        Timber.d("requestUsageStatsPermission: Requesting usage stats permission")
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    fun requestNotificationAccess(context: Context) {
        Timber.d("requestNotificationAccess: Requesting notification access permission")
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPostNotificationPermission(requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        Timber.d("requestPostNotificationPermission: Requesting post notification permission")
        requestPermissionLauncher.launch(arrayOf(POST_NOTIFICATIONS))
    }

    @SuppressLint("NewApi")
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
