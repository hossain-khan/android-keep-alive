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
import dev.hossain.keepalive.util.AppConfig.DEFAULT_APP_CHECK_INTERVAL
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun AppConfigScreen(
    navController: NavController,
    context: Context,
    settingsRepository: SettingsRepository = SettingsRepository(context),
) {
    val coroutineScope = rememberCoroutineScope()

    // Reading DataStore values
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = DEFAULT_APP_CHECK_INTERVAL)
    val isHealthCheckEnabled by settingsRepository.enableHealthCheckFlow.collectAsState(initial = false)
    val healthCheckUUID by settingsRepository.healthCheckUUIDFlow.collectAsState(initial = "")

    Timber.tag("AppConfigScreen")
        .i("appCheckInterval: $appCheckInterval, isHealthCheckEnabled: $isHealthCheckEnabled, healthCheckUUID: $healthCheckUUID")

    var appCheckIntervalValue by remember { mutableStateOf(appCheckInterval.toFloat()) }
    var isHealthCheckEnabledValue by remember { mutableStateOf(isHealthCheckEnabled) }
    var healthCheckUUIDValue by remember { mutableStateOf(healthCheckUUID) }
    var healthCheckUUIDError by remember { mutableStateOf<String?>(null) }

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

        // Enable Healthcheck Setting
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
}

fun isValidUUID(uuid: String): Boolean {
    val uuidRegex = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$".toRegex()
    return uuid.matches(uuidRegex)
}