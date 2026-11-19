package dev.hossain.keepalive.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Checks whether critical background permissions (battery optimization exemption, overlay, and usage stats)
 * remain granted, and alerts the user once per revocation transition (e.g., after an OEM OTA update).
 */
object PermissionHealthChecker {
    /**
     * Result of evaluating whether a permission-revocation alert should be emitted.
     */
    data class PermissionAlertDecision(
        val shouldAlert: Boolean,
        val nextWasGranted: Boolean,
        val nextAlertShown: Boolean,
    )

    /**
     * Pure state-transition helper for deduplicating permission-revoked alerts.
     *
     * - When all critical permissions are granted: clears [nextAlertShown] and marks [nextWasGranted] = true.
     * - When any critical permission is missing and the user had previously granted them (or already
     *   configured monitored apps): triggers [shouldAlert] = true once until permissions are restored.
     */
    internal fun evaluatePermissionAlertTransition(
        allCriticalGranted: Boolean,
        wasPreviouslyGranted: Boolean,
        alertAlreadyShown: Boolean,
        isAppConfigured: Boolean,
    ): PermissionAlertDecision {
        if (allCriticalGranted) {
            return PermissionAlertDecision(
                shouldAlert = false,
                nextWasGranted = true,
                nextAlertShown = false,
            )
        }

        val shouldAlert = (wasPreviouslyGranted || isAppConfigured) && !alertAlreadyShown
        return PermissionAlertDecision(
            shouldAlert = shouldAlert,
            nextWasGranted = wasPreviouslyGranted,
            nextAlertShown = alertAlreadyShown || shouldAlert,
        )
    }

    /**
     * Returns human-readable names of any missing critical background permissions.
     */
    fun getMissingCriticalPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!isBatteryOptimizationIgnored(context)) {
            missing.add("Battery Optimization Exemption")
        }
        if (!Settings.canDrawOverlays(context)) {
            missing.add("Display Over Other Apps")
        }
        if (!hasUsageStatsPermission(context)) {
            missing.add("Usage Access")
        }

        return missing
    }

    /**
     * Checks critical permissions during boot or watchdog execution and notifies/logs once
     * if any previously granted permission was silently revoked.
     */
    suspend fun checkAndAlertIfRevoked(
        context: Context,
        settingsRepository: SettingsRepository,
        activityLogger: AppActivityLogger,
        notificationHelper: NotificationHelper,
        isAppConfigured: Boolean,
    ) {
        val missingPermissions = getMissingCriticalPermissions(context)
        val wasGranted = settingsRepository.wasCriticalPermissionsGrantedFlow.first()
        val alertShown = settingsRepository.permissionRevokedAlertShownFlow.first()

        val decision =
            evaluatePermissionAlertTransition(
                allCriticalGranted = missingPermissions.isEmpty(),
                wasPreviouslyGranted = wasGranted,
                alertAlreadyShown = alertShown,
                isAppConfigured = isAppConfigured,
            )

        if (decision.nextWasGranted != wasGranted || decision.nextAlertShown != alertShown) {
            settingsRepository.savePermissionAlertState(
                wasGranted = decision.nextWasGranted,
                alertShown = decision.nextAlertShown,
            )
        }

        if (decision.shouldAlert) {
            val missingSummary = missingPermissions.joinToString(", ")
            Timber.w("Critical permissions revoked (possible OTA reset): $missingSummary")

            notificationHelper.showPermissionRevokedNotification(missingPermissions)

            activityLogger.logAppActivity(
                AppActivityLog(
                    packageId = context.packageName,
                    appName = "Keep Alive",
                    wasRunningRecently = false,
                    wasAttemptedToStart = false,
                    timestamp = System.currentTimeMillis(),
                    forceStartEnabled = false,
                    message = "Critical permission(s) missing or revoked (possible OTA reset): $missingSummary",
                ),
            )
        }
    }

    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
