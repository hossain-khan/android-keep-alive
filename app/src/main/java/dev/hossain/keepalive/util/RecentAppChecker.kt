package dev.hossain.keepalive.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import timber.log.Timber
import java.util.SortedMap
import java.util.TreeMap

/**
 * Utility object to check the recent usage of apps on the device.
 *
 * IMPORTANT PERMISSION REQUIREMENT:
 * This utility relies on the `android.permission.PACKAGE_USAGE_STATS` permission,
 * which must be granted by the user through system settings. Without this permission,
 * methods like `getRecentlyRunningAppStats` will likely return empty or unreliable data.
 * The application should ensure this permission is requested and granted.
 *
 * This object provides methods to determine if an app has been running recently,
 * retrieve recent app usage statistics, and check if an app is currently running in the foreground.
 */
object RecentAppChecker {
    /**
     * Checks if a specific app has been running recently.
     *
     * @param recentlyRunApps A list of [UsageStats] representing recently used apps.
     * @param packageName The package name of the app to check.
     * @return `true` if the app has been running recently, `false` otherwise.
     */
    fun isAppRunningRecently(
        recentlyRunApps: List<UsageStats>,
        packageName: String,
    ): Boolean {
        val didAppRanRecently = recentlyRunApps.any { it.packageName == packageName }
        Timber.d("isAppRunningRecently: $packageName = $didAppRanRecently")
        return didAppRanRecently
    }

    /**
     * Retrieves a list of recently running app usage statistics.
     *
     * @param context The [Context] to access the [UsageStatsManager].
     * @param timeSinceMs The time interval in milliseconds to look back for app usage. Default is 10 minutes.
     * @return A list of [UsageStats] for apps that have been used recently.
     */
    fun getRecentlyRunningAppStats(
        context: Context,
        timeSinceMs: Long = 600_000,
    ): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - timeSinceMs
        val appList =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                beginTime,
                endTime,
            )

        if (!appList.isNullOrEmpty()) {
            Timber.d(
                "getRecentlyRunningAppStats: Found ${appList.size} app events " +
                    "in last $timeSinceMs ms. First few: ${appList.take(5).joinToString { it.packageName + " (lastTimeUsed: " + it.lastTimeUsed + ")" }}",
            )
            return appList
        }
        return emptyList()
    }

    /**
     * Checks if a specific app is currently running in the foreground.
     *
     * @param context The [Context] to access the [UsageStatsManager].
     * @param packageName The package name of the app to check.
     * @return `true` if the app is running in the foreground, `false` otherwise.
     */
    fun isAppRunning(
        context: Context,
        packageName: String,
    ): Boolean {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 100 * 1000
        val appList =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                beginTime,
                endTime,
            )

        if (appList != null && appList.isNotEmpty()) {
            val sortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in appList) {
                sortedMap[usageStats.lastTimeUsed] = usageStats
            }

            // check if last two keys have the package name
            if (sortedMap.size >= 2) {
                val lastApp = sortedMap[sortedMap.lastKey()]
                val secondLastApp = sortedMap[sortedMap.headMap(sortedMap.lastKey()).lastKey()]
                if (lastApp != null && packageName == lastApp.packageName) {
                    return true
                } else if (secondLastApp != null && packageName == secondLastApp.packageName) {
                    return true
                }
            } else {
                Timber.d("appList is null or empty. %s", appList)
            }
        }
        return false
    }
}
