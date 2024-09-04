package dev.hossain.keepalive.data

enum class PermissionType {
    /**
     * Allows the app to run foreground services. This is required for the `WatchdogService` to keep running in the foreground and monitor other apps.
     */
    PERMISSION_FOREGROUND_SERVICE,

    /**
     * Allows the app to use special foreground services. This is used for services that have special use cases, such as the `WatchdogService`.
     */
    PERMISSION_FOREGROUND_SERVICE_SPECIAL_USE,

    /**
     * Allows the app to receive the `ACTION_BOOT_COMPLETED` broadcast. This is necessary to restart the `WatchdogService` when the device boots up.
     */
    PERMISSION_RECEIVE_BOOT_COMPLETED,

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
