package dev.hossain.keepalive.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import timber.log.Timber
import java.util.SortedMap
import java.util.TreeMap

object RecentAppChecker {
    fun isAppRunningRecently(
        recentlyRunApps: List<UsageStats>,
        packageName: String,
    ): Boolean {
        val didAppRanRecently = recentlyRunApps.any { it.packageName == packageName }
        Timber.d("isAppRunningRecently: $packageName = $didAppRanRecently")
        return didAppRanRecently
    }

    fun getRecentlyRunningAppStats(
        context: Context,
        timeSinceMs: Long = 600_000,
    ): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 * 1000
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

            // Return only the last 10 items from the sorted map
            val usageStats =
                sortedMap.entries
                    .reversed() // Reverse the order to get the last items first
                    .take(5) // Take most recent 5 items
                    .map { it.value } // Extract the UsageStats objects
                    .toList()

            Timber.d(
                "getRecentlyRunningAppStats: Found ${sortedMap.size} apps running " +
                    "in last $timeSinceMs ms = ${usageStats.map { it.packageName + " " + it.lastTimeUsed }}",
            )
            return usageStats
        }
        return emptyList()
    }

    /**
     * Check if the app is running in foreground.
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
