package dev.hossain.keepalive.util;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class AppChecker {

    public static boolean isAppRunning(Context context, String packageName) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long time = System.currentTimeMillis();
        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

        if (appList != null && !appList.isEmpty()) {
            SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }

            if (!sortedMap.isEmpty()) {
                UsageStats currentApp = sortedMap.get(sortedMap.lastKey());
                if (currentApp != null && packageName.equals(currentApp.getPackageName())) {
                    return true;
                }
            }
        } else {
            Log.d("AppChecker", "appList is null or empty. " + appList);
        }
        return false;
    }
}
