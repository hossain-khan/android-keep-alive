package dev.hossain.keepalive.data.model

/**
 * Enum representing the different types of actions that can be logged
 * for app monitoring activity.
 */
enum class LogActionType {
    /** All logs - no filter applied */
    ALL,

    /** App was started (either normal start or force start) */
    STARTED,

    /** App was already running - no action needed */
    ALREADY_RUNNING,

    /** Unknown/failure state */
    FAILED,
    ;

    /**
     * Returns a human-readable display name for this action type.
     */
    fun displayName(): String =
        when (this) {
            ALL -> "All"
            STARTED -> "Started"
            ALREADY_RUNNING -> "Already Running"
            FAILED -> "Failed/Unknown"
        }

    companion object {
        /**
         * Determines the action type for a given [AppActivityLog] entry.
         */
        fun fromLog(log: AppActivityLog): LogActionType =
            when {
                log.wasAttemptedToStart -> STARTED
                log.wasRunningRecently -> ALREADY_RUNNING
                else -> FAILED
            }
    }
}
