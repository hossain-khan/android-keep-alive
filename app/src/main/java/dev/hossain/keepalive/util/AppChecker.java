package dev.hossain.keepalive.util;

import static dev.hossain.keepalive.util.AppConfig.PHOTOS_APP_PACKAGE_NAME;
import static dev.hossain.keepalive.util.AppConfig.SYNC_APP_PACKAGE_NAME;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import timber.log.Timber;

public class AppChecker {

    public static boolean isGooglePhotosRunning(Context context) {
        return isAppRunning(context, PHOTOS_APP_PACKAGE_NAME);
    }
    public static boolean isSyncthingRunning(Context context) {
        return isAppRunning(context, SYNC_APP_PACKAGE_NAME);
    }

    /**
     * Check if the app is running in foreground.
     */
    private static boolean isAppRunning(Context context, String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long time = System.currentTimeMillis();
        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

        if (appList != null && !appList.isEmpty()) {
            SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }

            // check if last two keys have the package name
            if (sortedMap.size() >= 2) {
                UsageStats lastApp = sortedMap.get(sortedMap.lastKey());
                UsageStats secondLastApp = sortedMap.get(sortedMap.headMap(sortedMap.lastKey()).lastKey());
                if (lastApp != null && packageName.equals(lastApp.getPackageName())) {
                    return true;
                } else if (secondLastApp != null && packageName.equals(secondLastApp.getPackageName())) {
                    return true;
                }
            } else {
                Timber.d("appList is null or empty. %s", appList);
            }
        }
        return false;
    }

    private static void printSortedMap(SortedMap<Long, UsageStats> sortedMap) {
        for (Long key : sortedMap.keySet()) {
            UsageStats usageStats = sortedMap.get(key);
            Timber.d("UsageStats: " + usageStats.getPackageName() + " | " + usageStats.getLastTimeUsed());
        }
    }
}
