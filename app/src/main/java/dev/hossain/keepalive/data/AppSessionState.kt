package dev.hossain.keepalive.data

/**
 * Simple in-memory state holder for app session data.
 *
 * This object holds transient state that should be reset when the app process is restarted.
 * This is useful for features like dismissible warnings that should reappear after app restart.
 */
object AppSessionState {
    /**
     * Flag indicating whether the battery drain warning has been dismissed by the user
     * for the current app session.
     *
     * This value resets to `false` when the app process is restarted (e.g., after phone restart).
     */
    var isBatteryWarningDismissed: Boolean = false
}
