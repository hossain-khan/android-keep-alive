package dev.hossain.keepalive.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import timber.log.Timber
import java.util.SortedMap
import java.util.TreeMap

/**
 * Utility object for checking the recent usage of applications on the device.
 *
 * This object leverages the [UsageStatsManager] to query application usage statistics.
 * It provides methods to:
 * - Determine if a specific app has been used within a recent timeframe ([isAppRunningRecently]).
 * - Retrieve detailed [UsageStats] for recently used apps ([getRecentlyRunningAppStats]).
 * - Check if an app is currently considered to be "running" based on its last usage time ([isAppConsideredRunning]).
 *
 * Requires the `android.permission.PACKAGE_USAGE_STATS` permission, which is a special
 * permission that the user must grant through system settings.
 */
object RecentAppChecker {
    /**
     * Checks if a specific app is present in a given list of recently run [UsageStats].
     *
     * @param recentlyUsedAppStats A list of [UsageStats] objects, typically obtained from [getRecentlyRunningAppStats].
     * @param packageName The package name of the app to check.
     * @return `true` if an entry for the `packageName` exists in `recentlyUsedAppStats`, `false` otherwise.
     */
    fun isAppRunningRecently(
        recentlyUsedAppStats: List<UsageStats>,
        packageName: String,
    ): Boolean {
        val didAppRunRecently = recentlyUsedAppStats.any { it.packageName == packageName }
        Timber.d("isAppRunningRecently: $packageName = $didAppRunRecently (checked against ${recentlyUsedAppStats.size} recent stats)")
        return didAppRunRecently
    }

    /**
     * Retrieves a list of [UsageStats] for apps that have been used within a specified lookback period.
     *
     * This method queries the [UsageStatsManager] for app usage within the interval
     * `(endTime - lookbackIntervalMs, endTime)`. It then sorts these stats by `lastTimeUsed`
     * and returns a limited number of the most recent entries (currently takes top 5).
     *
     * Note: The actual data returned by `queryUsageStats` can be granular and might not
     * perfectly align with intuitive notions of "recently running." The interpretation
     * and filtering (like taking top 5) are specific to this method's implementation.
     *
     * @param context The [Context] required to access the [UsageStatsManager].
     * @param lookbackIntervalMs The duration in milliseconds to look back from the current time
     *                           to query usage statistics. Defaults to 10 minutes (600,000 ms).
     *                           The actual query uses a fixed 1,000,000 ms interval for `queryUsageStats`.
     *                           This parameter currently primarily influences logging rather than the query window itself.
     * @return A list of [UsageStats] for the most recently used apps, sorted by `lastTimeUsed` (descending).
     *         Returns an empty list if no usage stats are available or if the permission is not granted.
     */
    fun getRecentlyRunningAppStats(
        context: Context,
        lookbackIntervalMs: Long = 600_000, // Default: 10 minutes
    ): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        // NOTE: The 'beginTime' is fixed to endTime - 1,000,000 ms (approx 16.6 minutes)
        // irrespective of 'lookbackIntervalMs'. 'lookbackIntervalMs' is currently only used for logging.
        // This might be a point of confusion or a bug if 'lookbackIntervalMs' is expected to define the query window.
        val beginTime = endTime - 1_000_000 // Query window of ~16.6 minutes
        val appList =
            usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, // Tries to get the most fine-grained data available
                beginTime,
                endTime,
            )

        if (appList != null && appList.isNotEmpty()) {
            val sortedMap: SortedMap<Long, UsageStats> = TreeMap()
            for (usageStats in appList) {
                sortedMap[usageStats.lastTimeUsed] = usageStats
            }

            // Return only the last 10 items from the sorted map
            val usageStats =
                sortedMap.entries
                    .reversed() // Reverse the order to get the last items first
                    .take(5) // Take most recent 5 items
                    .map { it.value } // Extract the UsageStats objects
                    .toList()

            Timber.d(
                "getRecentlyRunningAppStats: Found ${sortedMap.size} apps with usage in the last ~16.6 mins. " +
                    "Returning top 5 based on lastTimeUsed. Queried with lookbackIntervalMs param: $lookbackIntervalMs ms. " +
                    "Result: ${usageStats.map { it.packageName + " (used at " + it.lastTimeUsed + ")" }}",
            )
            return usageStats
        }
        return emptyList()
    }

    /**
     * Checks if a specific app is considered "running" by looking at its recent usage statistics.
     *
     * This method queries [UsageStatsManager] for events in a short window (currently 100 seconds)
     * and checks if the target app is among the most recently used ones (specifically, if it's
     * the last or second-to-last app in the sorted list by `lastTimeUsed`).
     *
     * This is not a definitive check for "foreground app" but rather an indicator of very recent activity.
     * The reliability can vary based on OEM implementations and system behavior.
     *
     * @param context The [Context] to access the [UsageStatsManager].
     * @param packageName The package name of the app to check.
     * @return `true` if the app appears as one of the very recently used apps, `false` otherwise.
     */
    fun isAppConsideredRunning(
        context: Context,
        packageName: String,
    ): Boolean {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 100 * 1000 // Look back 100 seconds
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

            // Check if the target package name is among the most recent entries
            if (sortedMap.isNotEmpty()) {
                val lastApp = sortedMap[sortedMap.lastKey()]
                if (lastApp != null && packageName == lastApp.packageName) {
                    Timber.d("isAppConsideredRunning: $packageName is the most recent app.")
                    return true
                }
                if (sortedMap.size >= 2) {
                    // Get the second to last key safely
                    val secondLastKey = sortedMap.headMap(sortedMap.lastKey()).lastKey()
                    val secondLastApp = sortedMap[secondLastKey]
                    if (secondLastApp != null && packageName == secondLastApp.packageName) {
                        Timber.d("isAppConsideredRunning: $packageName is the second most recent app.")
                        return true
                    }
                }
            } else {
                Timber.d("isAppConsideredRunning: No usage stats found in the last 100 seconds.")
            }
        } else {
            Timber.d("isAppConsideredRunning: appList from UsageStatsManager is null or empty for the last 100 seconds.")
        }
        Timber.d("isAppConsideredRunning: $packageName is not considered running based on recent stats.")
        return false
    }
}
