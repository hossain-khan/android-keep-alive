package dev.hossain.keepalive.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import timber.log.Timber

/**
 * Utility object for launching other applications.
 */
object AppLauncher {
    /**
     * Opens the specified application using its package name.
     *
     * It retrieves the main launch intent for the package and starts it as a new task.
     * If no launch intent is found for the package, an error is logged.
     *
     * @param context The [Context] to use for accessing the [PackageManager] and starting the activity.
     * @param packageName The package name of the application to open.
     */
    fun openApp(
        context: Context,
        packageName: String,
    ) {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Timber.e("Unable to find launch intent for package: $packageName")
        }
    }

    /**
     * Starts a specific activity within an application package.
     *
     * This method is useful if you need to launch a non-default activity or be very specific.
     * It constructs an intent with `ACTION_MAIN` and `CATEGORY_LAUNCHER` for the given
     * package and activity name.
     *
     * Note: This method is currently private and unused. It can be made public if needed.
     *
     * @param context The [Context] to use for starting the activity.
     * @param packageName The package name of the application.
     * @param activityName The fully qualified class name of the activity to start.
     */
    private fun startSpecificActivity(
        context: Context,
        packageName: String,
        activityName: String,
    ) {
        // Start the activity
        val launchIntent = Intent()
        launchIntent.setAction(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.setComponent(
            ComponentName(packageName, activityName),
        )
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Unable to find activity: $activityName in package: $packageName")
        }
    }

    /**
     * Checks if a specific activity is available (exported and enabled) within a given package.
     *
     * Note: This method is currently private and unused. It can be made public if needed.
     *
     * @param context The [Context] to use for accessing the [PackageManager].
     * @param packageName The package name of the application.
     * @param activityName The fully qualified class name of the activity to check.
     * @return `true` if the activity is available, `false` otherwise.
     */
    private fun isActivityAvailable(
        context: Context,
        packageName: String,
        activityName: String,
    ): Boolean {
        val intent = Intent()
        intent.component = ComponentName(packageName, activityName)
        val packageManager = context.packageManager
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo != null
    }

    /**
     * Checks if an application with the given package name is installed on the device.
     *
     * Note: This method is currently private and unused. It can be made public if needed.
     *
     * @param context The [Context] to use for accessing the [PackageManager].
     * @param packageName The package name of the application to check.
     * @return `true` if the app is installed, `false` otherwise.
     */
    private fun isAppInstalled(
        context: Context,
        packageName: String,
    ): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
