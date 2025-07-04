package dev.hossain.keepalive.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the different screens in the app for navigation.
 *
 * @property route The navigation route for the screen.
 * @property title The display title of the screen.
 * @property icon The icon associated with the screen.
 */
enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    /** Home screen of the app. */
    Home("home", "Home", Icons.Filled.Home),

    /** Screen for managing app configurations. */
    WatchDogConfig("app_configs", "Config", Icons.Filled.Settings),

    /** Screen displaying the list of apps that are watched. */
    AppWatchList("app_watch_list", "Apps", Icons.AutoMirrored.Filled.List),

    /** Screen showing activity logs. */
    ActivityLogs("activity_logs", "Logs", Icons.Filled.Info),
}
