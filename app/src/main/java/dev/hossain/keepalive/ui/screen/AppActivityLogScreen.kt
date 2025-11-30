package dev.hossain.keepalive.ui.screen

import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.data.logging.AppActivityLogger
import dev.hossain.keepalive.data.model.AppActivityLog
import dev.hossain.keepalive.data.model.LogActionType
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var isRefreshing by remember { mutableStateOf(false) }

    // State for clear logs confirmation dialog
    var showClearLogsDialog by remember { mutableStateOf(false) }

    // Get view for haptic feedback
    val view = LocalView.current

    // Search and filter state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedActionType by rememberSaveable { mutableStateOf(LogActionType.ALL) }
    var selectedDateFilter by rememberSaveable { mutableStateOf(DateFilter.ALL) }

    // Filter logs based on search query, action type, and date range
    val filteredLogs by remember(logs, searchQuery, selectedActionType, selectedDateFilter) {
        derivedStateOf {
            filterLogs(logs, searchQuery, selectedActionType, selectedDateFilter)
        }
    }

    // Initialize SettingsRepository to get current settings
    val settingsRepository = remember { SettingsRepository(context) }
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL_MIN)
    val isForceStartAppsEnabled by settingsRepository.enableForceStartAppsFlow.collectAsState(
        initial = false,
    )

    // Clear logs confirmation dialog
    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = { Text("Clear All Logs") },
            text = {
                Text(
                    "Are you sure you want to clear all ${logs.size} activity logs? This action cannot be undone.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        showClearLogsDialog = false
                        isLoading.value = true
                        coroutineScope.launch {
                            activityLogger.clearLogs()
                            isLoading.value = false
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Activity Logs") },
                actions = {
                    // Export button
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            coroutineScope.launch {
                                exportLogsToFile(context, filteredLogs)
                            }
                        },
                        enabled = filteredLogs.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Logs",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    // The logs are collected via Flow from DataStore, so they're always up-to-date.
                    // This refresh provides visual feedback that the list has been checked.
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
            ) {
                // Search bar
                SearchBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips row
                FilterChipsRow(
                    selectedActionType = selectedActionType,
                    onActionTypeSelected = { selectedActionType = it },
                    selectedDateFilter = selectedDateFilter,
                    onDateFilterSelected = { selectedDateFilter = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Header subtitle with count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Showing ${filteredLogs.size} of ${logs.size} logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Clear logs button - now shows confirmation dialog
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showClearLogsDialog = true
                        },
                        enabled = !isLoading.value && logs.isNotEmpty(),
                    ) {
                        Text("Clear Logs")
                    }
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

                    EmptyStateMessage(
                        title = "No Activity Logs Yet",
                        message = "Logs will appear here after the watchdog service checks your monitored apps.",
                        suggestion = "💡 Make sure you have added apps to the watchlist and the service is running.",
                    )
                } else if (filteredLogs.isEmpty()) {
                    // Show Settings Card even when no matching logs
                    CurrentSettingsCard(appCheckInterval, isForceStartAppsEnabled)

                    EmptyStateMessage(
                        title = "No Matching Logs",
                        message = "No logs match the current filters.",
                        suggestion = "💡 Try adjusting your search query or filter settings to see more results.",
                    )
                } else {
                    LazyColumn {
                        // Add Settings Card as first item
                        item {
                            CurrentSettingsCard(appCheckInterval, isForceStartAppsEnabled)
                        }

                        items(filteredLogs) { logEntry ->
                            ActivityLogItem(logEntry, context)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays an empty state message with title, description, and helpful suggestion.
 */
@Composable
private fun EmptyStateMessage(
    title: String,
    message: String,
    suggestion: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
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
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
fun ActivityLogItem(
    log: AppActivityLog,
    context: Context,
) {
    // Try to get the app icon
    val appIcon =
        remember(log.packageId) {
            try {
                context.packageManager.getApplicationIcon(log.packageId)
            } catch (e: Exception) {
                null
            }
        }

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
                // App icon and name row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    // App icon
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.toBitmap(width = 40, height = 40).asImageBitmap(),
                            contentDescription = "${log.appName} icon",
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // App name
                    Text(
                        text = log.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Status indicator
                StatusIcon(log)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Package: ${log.packageId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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

/**
 * Enum representing date filter options for logs.
 */
enum class DateFilter {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    ;

    fun displayName(): String =
        when (this) {
            ALL -> "All Time"
            TODAY -> "Today"
            LAST_7_DAYS -> "Last 7 Days"
            LAST_30_DAYS -> "Last 30 Days"
        }

    /**
     * Returns the start timestamp for this date filter.
     * Returns null for ALL (no filtering).
     */
    fun getStartTimestamp(): Long? {
        val calendar = Calendar.getInstance()
        return when (this) {
            ALL -> null
            TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
        }
    }
}

/**
 * Search bar composable for filtering logs by app name.
 */
@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by app name or package...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        singleLine = true,
    )
}

/**
 * Row of filter chips for action type and date filtering.
 */
@Composable
private fun FilterChipsRow(
    selectedActionType: LogActionType,
    onActionTypeSelected: (LogActionType) -> Unit,
    selectedDateFilter: DateFilter,
    onDateFilterSelected: (DateFilter) -> Unit,
) {
    Column {
        // Action type filters
        Text(
            text = "Filter by action:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LogActionType.entries.forEach { actionType ->
                FilterChip(
                    selected = selectedActionType == actionType,
                    onClick = { onActionTypeSelected(actionType) },
                    label = { Text(actionType.displayName()) },
                    leadingIcon =
                        if (selectedActionType == actionType) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date filters
        Text(
            text = "Filter by date:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DateFilter.entries.forEach { dateFilter ->
                FilterChip(
                    selected = selectedDateFilter == dateFilter,
                    onClick = { onDateFilterSelected(dateFilter) },
                    label = { Text(dateFilter.displayName()) },
                    leadingIcon =
                        if (selectedDateFilter == dateFilter) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}

/**
 * Filters logs based on search query, action type, and date range.
 */
private fun filterLogs(
    logs: List<AppActivityLog>,
    searchQuery: String,
    actionType: LogActionType,
    dateFilter: DateFilter,
): List<AppActivityLog> {
    var filtered = logs

    // Filter by search query (app name or package ID)
    if (searchQuery.isNotBlank()) {
        val query = searchQuery.lowercase()
        filtered =
            filtered.filter { log ->
                log.appName.lowercase().contains(query) ||
                    log.packageId.lowercase().contains(query)
            }
    }

    // Filter by action type
    if (actionType != LogActionType.ALL) {
        filtered =
            filtered.filter { log ->
                LogActionType.fromLog(log) == actionType
            }
    }

    // Filter by date
    val startTimestamp = dateFilter.getStartTimestamp()
    if (startTimestamp != null) {
        filtered = filtered.filter { log -> log.timestamp >= startTimestamp }
    }

    return filtered
}

/**
 * Exports filtered logs to a file and shares it.
 */
private suspend fun exportLogsToFile(
    context: Context,
    logs: List<AppActivityLog>,
) {
    withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val fileName = "activity_logs_$timestamp.txt"

            val exportDir = File(context.cacheDir, "exports")
            exportDir.mkdirs()
            val file = File(exportDir, fileName)

            val logContent =
                buildString {
                    appendLine("Keep Alive Activity Logs")
                    val exportDateFormat =
                        SimpleDateFormat("MMM d, yyyy 'at' h:mm:ss a", Locale.getDefault())
                    appendLine("Exported: ${exportDateFormat.format(Date())}")
                    appendLine("Total logs: ${logs.size}")
                    appendLine("=".repeat(50))
                    appendLine()

                    logs.forEach { log ->
                        appendLine("App: ${log.appName}")
                        appendLine("Package: ${log.packageId}")
                        appendLine("Status: ${log.getStatusSummary()}")
                        val logDateFormat =
                            SimpleDateFormat(
                                "MMM d, yyyy 'at' h:mm:ss a",
                                Locale.getDefault(),
                            )
                        appendLine("Time: ${logDateFormat.format(Date(log.timestamp))}")
                        if (log.message.isNotEmpty()) {
                            appendLine("Message: ${log.message}")
                        }
                        appendLine("-".repeat(30))
                        appendLine()
                    }
                }

            file.writeText(logContent)

            withContext(Dispatchers.Main) {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file,
                    )

                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Activity Logs").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to export logs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
