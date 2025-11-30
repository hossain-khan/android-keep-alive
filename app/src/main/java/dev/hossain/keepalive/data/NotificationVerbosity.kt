package dev.hossain.keepalive.data

/**
 * Enum representing the notification verbosity levels for the app.
 *
 * - [QUIET]: Only show essential notifications (service running status).
 * - [NORMAL]: Show notifications for important events (app restarts, service status).
 * - [VERBOSE]: Show detailed notifications including last check time and all events.
 */
enum class NotificationVerbosity {
    /** Only show essential notifications (service running status). */
    QUIET,

    /** Show notifications for important events (app restarts, service status). */
    NORMAL,

    /** Show detailed notifications including last check time and all events. */
    VERBOSE,
}
