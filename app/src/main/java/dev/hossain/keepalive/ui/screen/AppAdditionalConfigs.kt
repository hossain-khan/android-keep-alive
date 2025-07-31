package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.util.AppConfig.APP_CHECK_INTERVAL_STEP
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import dev.hossain.keepalive.util.AppConfig.MAX_APP_CHECK_INTERVAL_SLIDER
import dev.hossain.keepalive.util.AppConfig.MIN_APP_CHECK_INTERVAL_SLIDER
import dev.hossain.keepalive.util.ServiceManager
import dev.hossain.keepalive.util.Validator.isValidUUID
import dev.hossain.keepalive.util.Validator.isValidUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    navController: NavController,
    context: Context,
    settingsRepository: SettingsRepository = SettingsRepository(context),
) {
    val coroutineScope = rememberCoroutineScope()

    // Reading DataStore values
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL_MIN)
    val isForceStartAppsEnabled by settingsRepository.enableForceStartAppsFlow.collectAsState(
        initial = false,
    )
    val isHealthCheckEnabled by settingsRepository.enableHealthCheckFlow.collectAsState(initial = false)
    val healthCheckUUID by settingsRepository.healthCheckUUIDFlow.collectAsState(initial = "")

    // New state variables for Remote Logging settings
    val isRemoteLoggingEnabled by settingsRepository.enableRemoteLoggingFlow.collectAsState(initial = false)
    val airtableToken by settingsRepository.airtableTokenFlow.collectAsState(initial = "")
    val airtableDataUrl by settingsRepository.airtableDataUrlFlow.collectAsState(initial = "")

    var appCheckIntervalValue by remember { mutableStateOf(appCheckInterval.toFloat()) }
    var isForceStartAppsEnabledValue by remember { mutableStateOf(isForceStartAppsEnabled) }
    var isHealthCheckEnabledValue by remember { mutableStateOf(isHealthCheckEnabled) }
    var healthCheckUUIDValue by remember { mutableStateOf(healthCheckUUID) }
    var healthCheckUUIDError by remember { mutableStateOf<String?>(null) }

    // New mutable states for Remote Logging settings
    var isRemoteLoggingEnabledValue by remember { mutableStateOf(isRemoteLoggingEnabled) }
    var airtableTokenValue by remember { mutableStateOf(airtableToken) }
    var airtableDataUrlValue by remember { mutableStateOf(airtableDataUrl) }
    var airtableTokenError by remember { mutableStateOf<String?>(null) }
    var airtableDataUrlError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("App Configuration") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Check Interval Setting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "App Check Interval")
                Text(text = formatMinutesToHoursAndMinutes(appCheckIntervalValue.toInt()))
            }
            Text(
                text =
                    """
            |This app will check if configured apps have been recently used or opened.`.
            |If not, it will re-start those apps to foreground so that the app and it's services can run again.
                    """.trimMargin(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
            Slider(
                value = appCheckIntervalValue,
                onValueChange = { appCheckIntervalValue = it },
                valueRange = MIN_APP_CHECK_INTERVAL_SLIDER.toFloat()..MAX_APP_CHECK_INTERVAL_SLIDER.toFloat(),
                steps = (MAX_APP_CHECK_INTERVAL_SLIDER - MIN_APP_CHECK_INTERVAL_SLIDER) / APP_CHECK_INTERVAL_STEP - 1,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                onValueChangeFinished = {
                    // Save the value when the user stops dragging the slider
                    coroutineScope.launch {
                        val newInterval = appCheckIntervalValue.toInt()
                        // Only restart if the interval value has actually changed
                        if (newInterval != appCheckInterval) {
                            settingsRepository.saveAppCheckInterval(newInterval)
                            // Restart the WatchdogService to apply the new interval
                            ServiceManager.restartWatchdogService(context)
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Enable Force Start Apps Setting
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isForceStartAppsEnabledValue,
                    onCheckedChange = {
                        isForceStartAppsEnabledValue = it
                        coroutineScope.launch {
                            settingsRepository.saveEnableForceStartApps(it)
                        }
                    },
                )
                Column {
                    Text(text = "Enable Force Start Apps")
                    Text(
                        text =
                            """
                        |When enabled, the app will automatically start selected apps, even if they might have been running recently.
                        |This will ensure that the selected app is always attempted to be started at the interval specified above.
                            """.trimMargin(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enable Health Check Setting
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isHealthCheckEnabledValue,
                    onCheckedChange = {
                        isHealthCheckEnabledValue = it
                        coroutineScope.launch {
                            settingsRepository.saveEnableHealthCheck(it)
                        }
                    },
                )
                Column {
                    Text(text = "Enable Health Check")
                    Text(
                        text =
                            """
                        |When enabled, the app will send a ping to the server with the UUID. Ping sent to `https://hc-ping.com/{UUID}`.
                        |Ping will be sent at specified interval and only when your selected app(s) are validated to be alive or restart by this app.
                            """.trimMargin(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health Check Ping UUID (only enabled when healthcheck is enabled)
            if (isHealthCheckEnabledValue) {
                Column {
                    OutlinedTextField(
                        value = healthCheckUUIDValue,
                        onValueChange = {
                            healthCheckUUIDValue = it
                            if (isValidUUID(it)) {
                                healthCheckUUIDError = null
                                coroutineScope.launch {
                                    settingsRepository.saveHealthCheckUUID(it)
                                }
                            } else {
                                healthCheckUUIDError =
                                    "Invalid UUID format, please copy the UUID from healthchecks.io"
                            }
                        },
                        label = { Text("Health Check Ping UUID") },
                        placeholder = { Text("UUID format: xyz-mn-ab-pq-hijkl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = healthCheckUUIDError != null,
                    )
                    if (healthCheckUUIDError != null) {
                        Text(
                            text = healthCheckUUIDError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enable Remote Logging Setting
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isRemoteLoggingEnabledValue,
                    onCheckedChange = {
                        isRemoteLoggingEnabledValue = it
                        coroutineScope.launch {
                            settingsRepository.saveEnableRemoteLogging(it)
                        }
                    },
                )

                Column {
                    Text(text = "Enable Remote Logging")
                    Text(
                        text =
                            """
                        |When enabled, the app will send logs to Airtable using the provided token and table URL.
                        |Account required from airtable.com
                        |Additional guide is available at bit.ly/keep-alive-readme
                            """.trimMargin(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Airtable Token (only enabled when remote logging is enabled)
            if (isRemoteLoggingEnabledValue) {
                Column {
                    OutlinedTextField(
                        value = airtableTokenValue,
                        onValueChange = {
                            airtableTokenValue = it
                            if (it.isNotBlank()) {
                                airtableTokenError = null
                                coroutineScope.launch {
                                    settingsRepository.saveAirtableToken(it)
                                }
                            } else {
                                airtableTokenError = "Airtable token cannot be empty"
                            }
                        },
                        label = { Text("Airtable Token") },
                        placeholder = { Text("Enter your Airtable token") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = airtableTokenError != null,
                    )
                    if (airtableTokenError != null) {
                        Text(
                            text = airtableTokenError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Airtable Data URL (only enabled when remote logging is enabled)
                Column {
                    OutlinedTextField(
                        value = airtableDataUrlValue,
                        onValueChange = {
                            airtableDataUrlValue = it
                            if (isValidUrl(it)) {
                                airtableDataUrlError = null
                                coroutineScope.launch {
                                    settingsRepository.saveAirtableDataUrl(it)
                                }
                            } else {
                                airtableDataUrlError = "Invalid URL format"
                            }
                        },
                        label = { Text("Airtable Data URL") },
                        placeholder = { Text("https://api.airtable.com/v0/{baseId}/{table}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = airtableDataUrlError != null,
                    )
                    if (airtableDataUrlError != null) {
                        Text(
                            text = airtableDataUrlError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done Button
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Done")
            }
        }

        LaunchedEffect(appCheckInterval) {
            appCheckIntervalValue = appCheckInterval.toFloat()
        }

        LaunchedEffect(isForceStartAppsEnabled) {
            isForceStartAppsEnabledValue = isForceStartAppsEnabled
        }

        LaunchedEffect(isHealthCheckEnabled) {
            isHealthCheckEnabledValue = isHealthCheckEnabled
        }

        LaunchedEffect(healthCheckUUID) {
            healthCheckUUIDValue = healthCheckUUID
        }

        // New LaunchedEffects for Remote Logging settings
        LaunchedEffect(isRemoteLoggingEnabled) {
            isRemoteLoggingEnabledValue = isRemoteLoggingEnabled
        }

        LaunchedEffect(airtableToken) {
            airtableTokenValue = airtableToken
        }

        LaunchedEffect(airtableDataUrl) {
            airtableDataUrlValue = airtableDataUrl
        }
    }
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
