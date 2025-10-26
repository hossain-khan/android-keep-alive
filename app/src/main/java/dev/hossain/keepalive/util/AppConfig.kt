package dev.hossain.keepalive.util

object AppConfig {
    const val MINIMUM_APP_CHECK_INTERVAL_MIN = 5
    const val DEFAULT_APP_CHECK_INTERVAL_MIN = 30
    const val DELAY_BETWEEN_MULTIPLE_APP_CHECKS_MS = 10_000L

    // Slider configuration for app check interval
    const val MIN_APP_CHECK_INTERVAL_SLIDER = 10
    const val MAX_APP_CHECK_INTERVAL_SLIDER = 1440 // 24 hours in minutes
    const val APP_CHECK_INTERVAL_STEP = 5
}
