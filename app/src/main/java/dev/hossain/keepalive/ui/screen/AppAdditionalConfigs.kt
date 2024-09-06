package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import dev.hossain.keepalive.data.SettingsRepository
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun AppConfigScreen(
    context: Context,
    settingsRepository: SettingsRepository = SettingsRepository(context),
) {
    val coroutineScope = rememberCoroutineScope()

    // Reading DataStore values
    val appCheckInterval by settingsRepository.appCheckIntervalFlow.collectAsState(initial = 30)
    val isHealthCheckEnabled by settingsRepository.enableHealthCheckFlow.collectAsState(initial = false)
    val healthCheckUUID by settingsRepository.healthCheckUUIDFlow.collectAsState(initial = "")

    Timber.tag("AppConfigScreen")
        .i("appCheckInterval: $appCheckInterval, isHealthCheckEnabled: $isHealthCheckEnabled, healthCheckUUID: $healthCheckUUID")

    var appCheckIntervalValue by remember { mutableStateOf(appCheckInterval.toFloat()) }
    var isHealthCheckEnabledValue by remember { mutableStateOf(isHealthCheckEnabled) }
    var healthCheckUUIDValue by remember { mutableStateOf(healthCheckUUID) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "App Configurations",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // App Check Interval Setting
        Text(text = "App Check Interval")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${appCheckIntervalValue.toInt()} minutes") // Show selected interval
        Slider(
            value = appCheckIntervalValue,
            onValueChange = { appCheckIntervalValue = it },
            valueRange = 10f..180f,
            steps = 180 / 5,
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
                        """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Health Check Ping UUID (only enabled when healthcheck is enabled)
        if (isHealthCheckEnabledValue) {
            OutlinedTextField(
                value = healthCheckUUIDValue,
                onValueChange = {
                    healthCheckUUIDValue = it
                    coroutineScope.launch {
                        settingsRepository.saveHealthCheckUUID(it)
                    }
                },
                label = { Text("Health Check Ping UUID") },
                placeholder = { Text("UUID format: xyz-mn-ab-pq-hijkl") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
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
