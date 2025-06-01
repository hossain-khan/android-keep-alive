package dev.hossain.keepalive.data

/**
 * Defines the types of runtime permissions that the application may request from the user.
 *
 * See additional permissions in AndroidManifest.xml
 *
 * Notes:
 * - https://github.com/hossain-khan/android-keep-alive?tab=readme-ov-file#-questionable-permissions-required-%EF%B8%8F
 * - https://github.com/hossain-khan/android-keep-alive/issues/41
 */
enum class PermissionType {
    /**
     * Allows the app to request the user to ignore battery optimizations. This is required to ensure the app can run in the background without being killed by the system.
     */
    PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS,

    /**
     * Allows the app to post notifications. This is necessary to notify the user about the status of monitored apps.
     */
    PERMISSION_POST_NOTIFICATIONS,

    /**
     * Allows the app to access usage stats of other apps. This is required to check if specific apps are running.
     */
    PERMISSION_PACKAGE_USAGE_STATS,

    /**
     * Allows the app to draw overlays on top of other apps. This is necessary for the app to start activities from the background on newer versions of Android.
     */
    PERMISSION_SYSTEM_APPLICATION_OVERLAY,
}
