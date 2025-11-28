package dev.hossain.keepalive.ui.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hossain.keepalive.R
import dev.hossain.keepalive.data.AppSessionState
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.ui.theme.KeepAliveTheme
import dev.hossain.keepalive.util.BatteryUtil

/**
 * Main landing screen composable for the Keep Alive app.
 * Displays the app icon, heading, and manages permission and navigation logic.
 */
@Composable
fun MainLandingScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    activityResultLauncher: ActivityResultLauncher<Intent>?,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
    allPermissionsGranted: Boolean = false,
    permissionType: PermissionType,
    showPermissionRequestDialog: MutableState<Boolean>,
    onRequestPermissions: () -> Unit,
    totalRequiredCount: Int,
    grantedCount: Int,
    configuredAppsCount: Int,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
                    .padding(innerPadding),
        ) {
            Image(
                painter = painterResource(id = R.drawable.baseline_radar_24),
                contentDescription = "App Icon",
                modifier =
                    Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
            )
            AppHeading(
                title = "Keep Alive",
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
            )
            Text(
                text = "App that keeps other apps alive 💓",
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
            )
            Spacer(modifier = Modifier.height(128.dp))
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ℹ️ Required permission status \nApproved Permissions: $grantedCount of $totalRequiredCount")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (allPermissionsGranted) Icons.Filled.Check else Icons.Filled.Clear,
                        // Set color to red if permission is not granted
                        tint = if (allPermissionsGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        contentDescription = "Icon",
                    )
                }
                AnimatedVisibility(
                    visible = !allPermissionsGranted,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 32.dp),
                ) {
                    Button(
                        onClick = { onRequestPermissions() },
                    ) {
                        Text("Grant Permission")
                    }
                }
                AnimatedVisibility(
                    visible = allPermissionsGranted,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 32.dp),
                ) {
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Display subtitle with appropriate text based on configured app count
                        Text(
                            text =
                                if (configuredAppsCount == 0) {
                                    "No app added to watch list - Configure apps to get started"
                                } else {
                                    "Currently watching $configuredAppsCount apps"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (configuredAppsCount == 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                // Battery drain warning card
                val context = LocalContext.current
                var isCharging by remember { mutableStateOf(BatteryUtil.isCharging(context)) }
                var isDismissed by remember { mutableStateOf(AppSessionState.isBatteryWarningDismissed) }

                // Listen for battery state changes using a BroadcastReceiver
                DisposableEffect(context) {
                    val powerConnectionReceiver =
                        object : BroadcastReceiver() {
                            override fun onReceive(
                                receiverContext: Context?,
                                intent: Intent?,
                            ) {
                                isCharging = BatteryUtil.isCharging(context)
                            }
                        }

                    val intentFilter =
                        IntentFilter().apply {
                            addAction(Intent.ACTION_POWER_CONNECTED)
                            addAction(Intent.ACTION_POWER_DISCONNECTED)
                        }
                    context.registerReceiver(powerConnectionReceiver, intentFilter)

                    onDispose {
                        context.unregisterReceiver(powerConnectionReceiver)
                    }
                }

                AnimatedVisibility(
                    visible = allPermissionsGranted && !isCharging && !isDismissed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                ) {
                    BatteryWarningCard(
                        onDismiss = {
                            AppSessionState.isBatteryWarningDismissed = true
                            isDismissed = true
                        },
                    )
                }
            }
        }
    }

    PermissionDialogs(
        context = LocalContext.current,
        permissionType = permissionType,
        showDialog = showPermissionRequestDialog,
        activityResultLauncher = activityResultLauncher,
        requestPermissionLauncher = requestPermissionLauncher,
    )
}

@Composable
fun AppHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge,
    )
}

/**
 * Warning card displayed when the device is not plugged into a charger.
 * Warns users about potential battery drain from running the app.
 *
 * @param onDismiss Callback invoked when the user dismisses the warning.
 */
@Composable
fun BatteryWarningCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.battery_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.battery_warning_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.battery_warning_dismiss),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryWarningCardPreview() {
    KeepAliveTheme {
        BatteryWarningCard(onDismiss = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AppHeadingPreview() {
    KeepAliveTheme {
        AppHeading("Hello Android App")
    }
}
