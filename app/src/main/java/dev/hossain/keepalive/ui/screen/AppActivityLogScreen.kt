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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import java.util.concurrent.TimeUnit

/**
 * Displays the activity log screen showing recent app activity logs and settings.
 */
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

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ALL) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Filter logs based on search query and time frame
    val filteredLogs by remember {
        derivedStateOf {
            logs.filter { log ->
                val matchesSearch =
                    searchQuery.isEmpty() ||
                        log.appName.contains(searchQuery, ignoreCase = true) ||
                        log.packageId.contains(searchQuery, ignoreCase = true)

                val matchesTimeFrame =
                    when (selectedTimeFrame) {
                        TimeFrame.ALL -> true
                        TimeFrame.LAST_HOUR -> log.timestamp > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
                        TimeFrame.LAST_DAY -> log.timestamp > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                        TimeFrame.LAST_WEEK -> log.timestamp > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    }

                matchesSearch && matchesTimeFrame
            }
        }
    }

    // Initialize SettingsRepository to get current settings
    val settingsRepository = remember { SettingsRepository(context) }
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL_MIN)
    val isForceStartAppsEnabled by settingsRepository.enableForceStartAppsFlow.collectAsState(
        initial = false,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs") },
                actions = {
                    IconButton(
                        onClick = { showFilterMenu = !showFilterMenu },
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter logs",
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                    ) {
                        TimeFrame.values().forEach { timeFrame ->
                            DropdownMenuItem(
                                text = { Text(timeFrame.displayName) },
                                onClick = {
                                    selectedTimeFrame = timeFrame
                                    showFilterMenu = false
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedTimeFrame != TimeFrame.ALL,
                    onClick = { /* Already handled in TopAppBar dropdown */ },
                    label = { Text(selectedTimeFrame.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                        )
                    },
                )

                Spacer(modifier = Modifier.weight(1f))

                // Clear logs button with improved UX
                Button(
                    onClick = { showClearConfirmation = true },
                    enabled = !isLoading.value && logs.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Logs")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results count
            Text(
                text =
                    if (searchQuery.isNotEmpty() || selectedTimeFrame != TimeFrame.ALL) {
                        "Showing ${filteredLogs.size} of ${logs.size} logs"
                    } else {
                        "${logs.size} logs"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (filteredLogs.isEmpty()) {
                // Show empty state for filtered results
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No logs match your filters",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search or time frame filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn {
                    // Add Settings Card as first item
                    item {
                        CurrentSettingsCard(appCheckInterval, isForceStartAppsEnabled)
                    }

                    items(filteredLogs) { logEntry ->
                        ActivityLogItem(logEntry)
                        HorizontalDivider()
                    }
                }
            }
        }

        // Clear confirmation dialog
        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                title = { Text("Clear All Logs?") },
                text = {
                    Text("This will permanently delete all ${logs.size} activity logs. This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirmation = false
                            isLoading.value = true
                            coroutineScope.launch {
                                activityLogger.clearLogs()
                                isLoading.value = false
                            }
                        },
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmation = false },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

/**
 * Time frame options for filtering logs
 */
enum class TimeFrame(val displayName: String) {
    ALL("All Time"),
    LAST_HOUR("Last Hour"),
    LAST_DAY("Last Day"),
    LAST_WEEK("Last Week"),
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

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
                            MaterialTheme.colorScheme.onSurfaceVariant
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
        Column(modifier = Modifier.padding(16.dp)) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Package: ${log.packageId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status info
            Text(
                text = log.getStatusSummary(),
                style = MaterialTheme.typography.bodyMedium,
                color = getStatusColor(log),
            )

            if (log.message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = log.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            log.wasAttemptedToStart && log.forceStartEnabled -> MaterialTheme.colorScheme.primary
            log.wasAttemptedToStart -> Color(0xFFFF9800) // Orange - warning
            log.wasRunningRecently -> Color(0xFF4CAF50) // Green - success
            else -> MaterialTheme.colorScheme.error
        }

    Icon(
        imageVector = icon,
        contentDescription = "Status",
        tint = tint,
    )
}

@Composable
private fun getStatusColor(log: AppActivityLog): Color {
    return when {
        log.wasAttemptedToStart && log.forceStartEnabled -> MaterialTheme.colorScheme.primary
        log.wasAttemptedToStart -> Color(0xFFFF9800) // Orange - warning
        log.wasRunningRecently -> Color(0xFF4CAF50) // Green - success
        else -> MaterialTheme.colorScheme.error
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
