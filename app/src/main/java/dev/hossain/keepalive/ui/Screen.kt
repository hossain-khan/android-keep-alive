package dev.hossain.keepalive.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Filled.Home),
    AppConfigs("app_configs", "Config", Icons.Filled.Settings),
    AppSettings("settings", "Apps", Icons.AutoMirrored.Filled.List),
    ActivityLogs("activity_logs", "Logs", Icons.Filled.Info),
}
