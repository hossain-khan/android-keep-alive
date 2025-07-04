package dev.hossain.keepalive.ui.screen

import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.hossain.keepalive.R
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.ui.theme.KeepAliveTheme

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
                                    "No app added to watch list. \nGo to 'App Settings' to add apps."
                                } else {
                                    "Watching $configuredAppsCount apps"
                                },
                            style = MaterialTheme.typography.bodyLarge,
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

@Preview(showBackground = true)
@Composable
fun AppHeadingPreview() {
    KeepAliveTheme {
        AppHeading("Hello Android App")
    }
}
