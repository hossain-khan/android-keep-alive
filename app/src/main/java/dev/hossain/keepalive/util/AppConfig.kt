package dev.hossain.keepalive.util

object AppConfig {
    const val MINIMUM_APP_CHECK_INTERVAL_MIN = 5
    const val MINIMUM_APP_CHECK_INTERVAL_SEC = 1
    const val DEFAULT_APP_CHECK_INTERVAL_MIN = 30
    const val DEFAULT_APP_CHECK_INTERVAL_SEC = 10
    const val DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS = 10_000L
    const val DELAY_BETWEEN_MULTIPLE_APP_FAST_CHECKS_MS = 1_000L
    const val RECENT_APP_RUN_FRESHNESS_DURATION_MS = 10_000L
}
