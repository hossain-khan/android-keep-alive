package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActivityLogScreen(
    navController: NavController,
    context: Context,
) {
    val activityLogger = remember { AppActivityLogger(context) }
    val logs by activityLogger.getRecentLogs().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val isLoading = remember { mutableStateOf(false) }

    // Initialize SettingsRepository to get current settings
    val settingsRepository = remember { SettingsRepository(context) }
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL_MIN)
    val isForceStartAppsEnabled by settingsRepository.enableForceStartAppsFlow.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            // Header subtitle
            Text(
            text = "Recent app monitoring activity:",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Clear logs button
        Button(
            onClick = {
                isLoading.value = true
                coroutineScope.launch {
                    activityLogger.clearLogs()
                    isLoading.value = false
                }
            },
            enabled = !isLoading.value && logs.isNotEmpty(),
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Clear Logs")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading.value) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                CircularProgressIndicator()
            }
        } else if (logs.isEmpty()) {
            // Show Settings Card even when no logs
            CurrentSettingsCard(appCheckInterval, isForceStartAppsEnabled)

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
            ) {
                Text(
                    text = "No activity logs yet. Logs will appear after the watchdog service checks monitored apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
        } else {
            LazyColumn {
                // Add Settings Card as first item
                item {
                    CurrentSettingsCard(appCheckInterval, isForceStartAppsEnabled)
                }

                items(logs) { logEntry ->
                    ActivityLogItem(logEntry)
                    Divider()
                }
            }
        }
        }
    }
}

@Composable
fun CurrentSettingsCard(
    appCheckInterval: Int,
    isForceStartAppsEnabled: Boolean,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Check Interval:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatMinutesToHoursAndMinutes(appCheckInterval),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Force Start Apps:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (isForceStartAppsEnabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isForceStartAppsEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Gray
                        },
                )
            }
        }
    }
}

@Composable
fun ActivityLogItem(log: AppActivityLog) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // App name
                Text(
                    text = log.appName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Status indicator
                StatusIcon(log)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Package: ${log.packageId}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Status info
            Text(
                text = log.getStatusSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = getStatusColor(log),
            )

            if (log.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp
            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun StatusIcon(log: AppActivityLog) {
    val icon =
        when {
            log.wasAttemptedToStart && log.forceStartEnabled -> Icons.Default.PlayArrow
            log.wasAttemptedToStart -> Icons.Default.Warning
            log.wasRunningRecently -> Icons.Default.Check
            else -> Icons.Default.Warning
        }

    val tint =
        when {
            log.wasAttemptedToStart && log.forceStartEnabled -> Color(0xFF2196F3) // Blue
            log.wasAttemptedToStart -> Color(0xFFFF9800) // Orange
            log.wasRunningRecently -> Color(0xFF4CAF50) // Green
            else -> Color(0xFFF44336) // Red
        }

    Icon(
        imageVector = icon,
        contentDescription = "Status",
        tint = tint,
    )
}

private fun getStatusColor(log: AppActivityLog): Color {
    return when {
        log.wasAttemptedToStart && log.forceStartEnabled -> Color(0xFF2196F3) // Blue
        log.wasAttemptedToStart -> Color(0xFFFF9800) // Orange
        log.wasRunningRecently -> Color(0xFF4CAF50) // Green
        else -> Color(0xFFF44336) // Red
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm:ss a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Formats a time in minutes to a readable string representation.
 * When the time is less than 60 minutes, it's displayed as "X minutes".
 * When the time is 60 minutes or more, it's displayed as "X hours Y minutes".
 *
 * @param minutes The time in minutes to format
 * @return A formatted string representing the time in hours and minutes
 */
private fun formatMinutesToHoursAndMinutes(minutes: Int): String {
    if (minutes < 60) {
        return "$minutes minutes"
    }

    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return if (remainingMinutes == 0) {
        if (hours == 1) "$hours hour" else "$hours hours"
    } else {
        val hoursText = if (hours == 1) "$hours hour" else "$hours hours"
        val minutesText = if (remainingMinutes == 1) "$remainingMinutes minute" else "$remainingMinutes minutes"
        "$hoursText $minutesText"
    }
}
