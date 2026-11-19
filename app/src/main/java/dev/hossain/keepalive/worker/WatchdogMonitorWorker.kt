package dev.hossain.keepalive.worker

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.util.ServiceManager
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic [CoroutineWorker] that acts as a self-healing backup layer for [WatchdogService].
 *
 * If an OEM battery manager kills [WatchdogService] despite `START_STICKY`, this worker detects
 * that the service is no longer running (or has become stale) and attempts to restart it.
 */
class WatchdogMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    /**
     * Decision result from evaluating whether [WatchdogService] needs to be started or restarted.
     */
    internal data class RestartDecision(
        val shouldStartOrRestart: Boolean,
        val forceRestart: Boolean = false,
        val reason: String,
    )

    companion object {
        const val UNIQUE_WORK_NAME = "WatchdogMonitorPeriodicWork"
        private const val REPEAT_INTERVAL_MINUTES = 15L
        private const val MIN_STALE_THRESHOLD_MINUTES = 30L

        /**
         * Ensures the periodic watchdog monitor work is scheduled without resetting an existing timer.
         */
        fun ensureScheduled(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            try {
                val existingWork = workManager.getWorkInfosForUniqueWork(UNIQUE_WORK_NAME).get()
                val isAlreadyActive =
                    existingWork.any {
                        it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                    }
                if (isAlreadyActive) {
                    Timber.d("WatchdogMonitorWorker is already scheduled; skipping re-enqueue.")
                    return
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to query existing WorkInfos; proceeding with KEEP policy.")
            }

            val workRequest =
                PeriodicWorkRequestBuilder<WatchdogMonitorWorker>(
                    REPEAT_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest,
            )
            Timber.d("Scheduled WatchdogMonitorWorker with ExistingPeriodicWorkPolicy.KEEP.")
        }

        /**
         * Evaluates whether [WatchdogService] should be started or restarted.
         */
        internal fun evaluateRestartDecision(
            isServiceRunning: Boolean,
            isMonitoringPaused: Boolean,
            hasConfiguredApps: Boolean,
            lastCheckTimeMs: Long,
            checkIntervalMin: Int,
            nowMs: Long,
        ): RestartDecision {
            if (!hasConfiguredApps) {
                return RestartDecision(
                    shouldStartOrRestart = false,
                    reason = "No apps configured for monitoring",
                )
            }
            if (isMonitoringPaused) {
                return RestartDecision(
                    shouldStartOrRestart = false,
                    reason = "Monitoring is paused by user",
                )
            }
            if (!isServiceRunning) {
                return RestartDecision(
                    shouldStartOrRestart = true,
                    forceRestart = false,
                    reason = "WatchdogService is not running",
                )
            }

            val staleThresholdMs =
                TimeUnit.MINUTES.toMillis(maxOf(checkIntervalMin.toLong() * 2, MIN_STALE_THRESHOLD_MINUTES))
            if (lastCheckTimeMs > 0L && (nowMs - lastCheckTimeMs) > staleThresholdMs) {
                return RestartDecision(
                    shouldStartOrRestart = true,
                    forceRestart = true,
                    reason = "WatchdogService last check is stale (${TimeUnit.MILLISECONDS.toMinutes(nowMs - lastCheckTimeMs)}m ago)",
                )
            }

            return RestartDecision(
                shouldStartOrRestart = false,
                reason = "WatchdogService is healthy",
            )
        }
    }

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        val activityLogger = AppActivityLogger(applicationContext)
        val configuredApps = AppDataStore.store(applicationContext).data.first()

        val decision =
            evaluateRestartDecision(
                isServiceRunning = WatchdogService.isRunning,
                isMonitoringPaused = settingsRepository.isMonitoringPausedFlow.first(),
                hasConfiguredApps = configuredApps.isNotEmpty(),
                lastCheckTimeMs = settingsRepository.lastCheckTimeFlow.first(),
                checkIntervalMin = settingsRepository.appCheckIntervalFlow.first(),
                nowMs = System.currentTimeMillis(),
            )

        if (!decision.shouldStartOrRestart) {
            Timber.d("WatchdogMonitorWorker: ${decision.reason}")
            return Result.success()
        }

        Timber.w("WatchdogMonitorWorker triggering recovery: ${decision.reason}")
        try {
            if (decision.forceRestart) {
                ServiceManager.restartWatchdogService(applicationContext)
            } else {
                ServiceManager.startWatchdogService(applicationContext)
            }

            activityLogger.logAppActivity(
                AppActivityLog(
                    packageId = applicationContext.packageName,
                    appName = "Keep Alive",
                    wasRunningRecently = false,
                    wasAttemptedToStart = true,
                    timestamp = System.currentTimeMillis(),
                    forceStartEnabled = decision.forceRestart,
                    message = "WorkManager recovery started WatchdogService (${decision.reason})",
                ),
            )
        } catch (e: Exception) {
            val errorType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                    "ForegroundServiceStartNotAllowedException"
                } else {
                    e::class.java.simpleName
                }
            Timber.e(e, "WatchdogMonitorWorker failed to start WatchdogService ($errorType)")
            activityLogger.logAppActivity(
                AppActivityLog(
                    packageId = applicationContext.packageName,
                    appName = "Keep Alive",
                    wasRunningRecently = false,
                    wasAttemptedToStart = true,
                    timestamp = System.currentTimeMillis(),
                    forceStartEnabled = decision.forceRestart,
                    message = "WorkManager recovery failed ($errorType): ${e.message}",
                ),
            )
        }

        return Result.success()
    }
}
