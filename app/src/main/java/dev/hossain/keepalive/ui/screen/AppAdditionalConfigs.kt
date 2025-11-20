package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.ui.theme.ThemeMode
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
    val launchAppsOnBoot by settingsRepository.launchAppsOnBootFlow.collectAsState(initial = false)
    val isHealthCheckEnabled by settingsRepository.enableHealthCheckFlow.collectAsState(initial = false)
    val healthCheckUUID by settingsRepository.healthCheckUUIDFlow.collectAsState(initial = "")

    // New state variables for Remote Logging settings
    val isRemoteLoggingEnabled by settingsRepository.enableRemoteLoggingFlow.collectAsState(initial = false)
    val airtableToken by settingsRepository.airtableTokenFlow.collectAsState(initial = "")
    val airtableDataUrl by settingsRepository.airtableDataUrlFlow.collectAsState(initial = "")

    // Theme mode setting
    val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

    var appCheckIntervalValue by remember { mutableStateOf(appCheckInterval.toFloat()) }
    var isForceStartAppsEnabledValue by remember { mutableStateOf(isForceStartAppsEnabled) }
    var launchAppsOnBootValue by remember { mutableStateOf(launchAppsOnBoot) }
    var isHealthCheckEnabledValue by remember { mutableStateOf(isHealthCheckEnabled) }
    var healthCheckUUIDValue by remember { mutableStateOf(healthCheckUUID) }
    var healthCheckUUIDError by remember { mutableStateOf<String?>(null) }

    // New mutable states for Remote Logging settings
    var isRemoteLoggingEnabledValue by remember { mutableStateOf(isRemoteLoggingEnabled) }
    var airtableTokenValue by remember { mutableStateOf(airtableToken) }
    var airtableDataUrlValue by remember { mutableStateOf(airtableDataUrl) }
    var airtableTokenError by remember { mutableStateOf<String?>(null) }
    var airtableDataUrlError by remember { mutableStateOf<String?>(null) }

    // Theme mode mutable state
    var selectedThemeMode by remember { mutableStateOf(themeMode) }

    Scaffold(
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

            Spacer(modifier = Modifier.height(24.dp))

            // Launch Apps on Boot Setting
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = launchAppsOnBootValue,
                    onCheckedChange = {
                        launchAppsOnBootValue = it
                        coroutineScope.launch {
                            settingsRepository.saveLaunchAppsOnBoot(it)
                        }
                    },
                )
                Column {
                    Text(text = "Launch apps immediately on boot")
                    Text(
                        text =
                            """
                        |When enabled, all configured apps will be launched immediately after the device boots up.
                        |Note: Device must be unlocked after boot for this to work (see known issue #70).
                            """.trimMargin(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

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

            Spacer(modifier = Modifier.height(32.dp))

            // Sticky App Selection Setting
            val appViewModel = remember { AppViewModel(AppDataStore.store(context)) }
            val monitoredApps by appViewModel.appList.observeAsState(emptyList())
            val currentStickyApp by appViewModel.stickyApp.observeAsState()

            Text(
                text = "Sticky App",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text =
                    """
                    |The sticky app will be launched last during each monitoring cycle, 
                    |bringing it to the top of the recent apps list and ensuring it remains the most recently active app. 
                    |This is useful for apps that need to stay prominent or accessible. Only one app can be sticky at a time.
                    """.trimMargin(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (monitoredApps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        ),
                ) {
                    Text(
                        text = "No apps are being monitored yet. Add apps from the main screen to enable sticky app selection.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Option for no sticky app
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentStickyApp == null,
                                        onClick = {
                                            currentStickyApp?.let { stickyApp ->
                                                appViewModel.toggleStickyApp(stickyApp)
                                            }
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = currentStickyApp == null,
                                onClick = null,
                            )
                            Text(
                                text = "No sticky app",
                                modifier = Modifier.padding(start = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        // List of monitored apps
                        monitoredApps.forEach { app ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = currentStickyApp?.packageName == app.packageName,
                                            onClick = {
                                                appViewModel.toggleStickyApp(app)
                                            },
                                            role = Role.RadioButton,
                                        )
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = currentStickyApp?.packageName == app.packageName,
                                    onClick = null,
                                )
                                Column(modifier = Modifier.padding(start = 16.dp)) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Theme Selection Setting
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Choose the appearance theme for the app",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Column {
                ThemeMode.values().forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedThemeMode == mode,
                                    onClick = {
                                        selectedThemeMode = mode
                                        coroutineScope.launch {
                                            settingsRepository.saveThemeMode(mode)
                                        }
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedThemeMode == mode,
                            // Handled by the parent Row's onClick
                            onClick = null,
                        )
                        Text(
                            text =
                                when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System default"
                                },
                            modifier = Modifier.padding(start = 16.dp),
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

        LaunchedEffect(themeMode) {
            selectedThemeMode = themeMode
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
