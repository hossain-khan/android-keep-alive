package dev.hossain.keepalive.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import timber.log.Timber

object AppLauncher {
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

    private fun startApplication(
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
            e.printStackTrace()
            Timber.e(e, "Unable to find activity: $launchIntent")
        }
    }

    /**
     * Checks if the activity is available in the package.
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
     * Checks if the app is installed.
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
