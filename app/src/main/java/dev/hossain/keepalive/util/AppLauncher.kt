package dev.hossain.keepalive.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_LAUNCH_ACTIVITY
import dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_PACKAGE_NAME
import dev.hossain.keepalive.util.AppConfig.SYNC_APP_LAUNCH_ACTIVITY
import dev.hossain.keepalive.util.AppConfig.SYNC_APP_PACKAGE_NAME

object AppLauncher {
    private const val TAG = "AppLauncher"

    fun openGooglePhotos(context: Context) {
        Log.i(TAG, "openGooglePhotos")
        startApplication(context, PHOTOS_APP_PACKAGE_NAME, PHOTOS_APP_LAUNCH_ACTIVITY)
    }

    fun openSyncthing(context: Context) {
        Log.i(TAG, "openSyncthing")
        startApplication(context, SYNC_APP_PACKAGE_NAME, SYNC_APP_LAUNCH_ACTIVITY)
    }

    private fun startApplication(context: Context, packageName: String, activityName: String) {
        // Start the activity
        val launchIntent = Intent()
        launchIntent.setAction(Intent.ACTION_MAIN)
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        launchIntent.setComponent(
            ComponentName(packageName, activityName)
        )
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Log.e(TAG, "Unable to find activity: $launchIntent", e)
        }
    }
}