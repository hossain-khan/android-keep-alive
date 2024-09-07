package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL_MIN
import kotlinx.coroutines.launch

@Composable
fun AppConfigScreen(
    navController: NavController,
    context: Context,
    settingsRepository: SettingsRepository = SettingsRepository(context),
) {
    val coroutineScope = rememberCoroutineScope()

    // Reading DataStore values
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL_MIN)
    val isHealthCheckEnabled by settingsRepository.enableHealthCheckFlow.collectAsState(initial = false)
    val healthCheckUUID by settingsRepository.healthCheckUUIDFlow.collectAsState(initial = "")

    // New state variables for Remote Logging settings
    val isRemoteLoggingEnabled by settingsRepository.enableRemoteLoggingFlow.collectAsState(initial = false)
    val airtableToken by settingsRepository.airtableTokenFlow.collectAsState(initial = "")
    val airtableDataUrl by settingsRepository.airtableDataUrlFlow.collectAsState(initial = "")

    var appCheckIntervalValue by remember { mutableStateOf(appCheckInterval.toFloat()) }
    var isHealthCheckEnabledValue by remember { mutableStateOf(isHealthCheckEnabled) }
    var healthCheckUUIDValue by remember { mutableStateOf(healthCheckUUID) }
    var healthCheckUUIDError by remember { mutableStateOf<String?>(null) }

    // New mutable states for Remote Logging settings
    var isRemoteLoggingEnabledValue by remember { mutableStateOf(isRemoteLoggingEnabled) }
    var airtableTokenValue by remember { mutableStateOf(airtableToken) }
    var airtableDataUrlValue by remember { mutableStateOf(airtableDataUrl) }
    var airtableTokenError by remember { mutableStateOf<String?>(null) }
    var airtableDataUrlError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "App Configurations",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // App Check Interval Setting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "App Check Interval")
            Text(text = "${appCheckIntervalValue.toInt()} minutes")
        }
        Slider(
            value = appCheckIntervalValue,
            onValueChange = { appCheckIntervalValue = it },
            valueRange = 10f..180f,
            steps = (180 - 10) / 5 - 1,
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = {
                // Save the value when the user stops dragging the slider
                coroutineScope.launch {
                    settingsRepository.saveAppCheckInterval(appCheckIntervalValue.toInt())
                }
            },
        )

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
                            healthCheckUUIDError = "Invalid UUID format, please copy the UUID from healthchecks.io"
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

        Spacer(modifier = Modifier.height(16.dp))

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

fun isValidUrl(url: String): Boolean {
    val urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$".toRegex()
    return url.matches(urlRegex)
}

fun isValidUUID(uuid: String): Boolean {
    val uuidRegex = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$".toRegex()
    return uuid.matches(uuidRegex)
}
