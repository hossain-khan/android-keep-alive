package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.ui.theme.KeepAliveTheme
import timber.log.Timber

/**
 * Main activity that launches the WatchdogService and requests for necessary permissions.
 */
class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepAliveTheme {
                val allPermissionsGranted by mainViewModel.allPermissionsGranted.observeAsState(false)

                MainLandingScreen(allPermissionsGranted = allPermissionsGranted)
            }
        }

        mainViewModel.checkAllPermissions(this)

        requestPermissionLauncher =
            this.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                if (permissions[POST_NOTIFICATIONS] == true) {
                    // Permission granted, you can post notifications
                    Timber.d("registerForActivityResult: POST_NOTIFICATIONS Permission granted")
                } else {
                    // Permission denied, handle accordingly
                    Timber.d("registerForActivityResult: POST_NOTIFICATIONS Permission denied")
                }

                if (permissions[PACKAGE_USAGE_STATS] == true) {
                    // Permission granted, you can get package usage stats
                    Timber.d("registerForActivityResult: PACKAGE_USAGE_STATS Permission granted")
                } else {
                    // Permission denied, handle accordingly
                    Timber.d("registerForActivityResult: PACKAGE_USAGE_STATS Permission denied")
                }
            }

        // Initialize the ActivityResultLauncher
        activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    if (Settings.canDrawOverlays(this)) {
                        // Permission granted, proceed with your action
                        Timber.d("registerForActivityResult: Overlay permission granted")
                    } else {
                        // Permission not granted, show a message to the user
                        Timber.d("registerForActivityResult: Overlay permission denied")
                    }
                }
            }

        if (mainViewModel.arePermissionsGranted(this, mainViewModel.requiredPermissions)) {
            Timber.d("onCreate arePermissionsGranted: All other permissions granted")
        } else {
            Timber.d("onCreate arePermissionsGranted: All other permissions not granted")
            requestPermissionLauncher.launch(mainViewModel.requiredPermissions)
        }

        if (mainViewModel.hasUsageStatsPermission(this)) {
            Timber.d("hasUsageStatsPermission: PACKAGE_USAGE_STATS Permission granted")
        } else {
            Timber.d("hasUsageStatsPermission: PACKAGE_USAGE_STATS Permission denied")
            requestUsageStatsPermission()
        }

        if (mainViewModel.hasOverlayPermission(this)) {
            // Permission granted, you can start the activity or service that needs this permission
        } else {
            // Permission not granted, request it
            requestOverlayPermission()
        }

        if (!mainViewModel.isBatteryOptimizationIgnored(this)) {
            showBatteryOptimizationDialog(this)
        } else {
            Timber.d("Battery optimization is already disabled for this app.")
        }
    }

    private fun showBatteryOptimizationDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Disable Battery Optimization")
            .setMessage("This app requires to be excluded from battery optimizations to function properly in the background.")
            .setPositiveButton("Exclude") { _, _ ->
                requestBatteryOptimizationExclusion(context)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        startWatchdogService()
    }

    private fun requestUsageStatsPermission() {
        Timber.d("requestUsageStatsPermission: Requesting usage stats permission")
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun startWatchdogService() {
        val serviceIntent = Intent(this, WatchdogService::class.java)
        startService(serviceIntent)
    }

    /**
     * When your app's WindowStopped is set to true, it means that your app's activity has been stopped,
     * which typically occurs when the app is no longer visible to the user. Starting a new activity
     * when your app's WindowStopped is true is restricted on newer versions of Android due to the
     * background activity launch restrictions.
     *
     * Understanding the Restriction
     * Starting from Android 10 (API level 29), apps are restricted from launching activities from
     * the background to improve the user experience and reduce unexpected interruptions.
     */
    private fun requestOverlayPermission() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
        activityResultLauncher.launch(intent)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExclusion(context: Context) {
        Toast.makeText(
            context,
            "Please exclude this app from battery optimization.",
            Toast.LENGTH_SHORT,
        ).show()
        val intent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        context.startActivity(intent)
    }
}

@Composable
fun MainLandingScreen(
    modifier: Modifier = Modifier,
    allPermissionsGranted: Boolean = false,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ℹ️ Required permission status")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (allPermissionsGranted) Icons.Filled.Check else Icons.Filled.Clear,
                        // Set color to red if permission is not granted
                        tint = if (allPermissionsGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        contentDescription = "Icon",
                    )
                }
                if (!allPermissionsGranted) {
                    Button(
                        onClick = { /* Handle permission grant */ },
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 32.dp),
                    ) {
                        Text("Grant Permissions")
                    }
                }
            }
        },
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
                text = "App that keeps photos and sync apps alive.",
                style = MaterialTheme.typography.bodyLarge,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainLandingScreenPreview() {
    KeepAliveTheme {
        MainLandingScreen(allPermissionsGranted = true)
    }
}

@Preview(showBackground = true)
@Composable
fun MainLandingScreenPreviewWithoutButton() {
    KeepAliveTheme {
        MainLandingScreen(allPermissionsGranted = false)
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialog(
    showDialog: Boolean,
    title: String,
    description: String,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onCancel,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = description)
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onAccept) {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MyBottomSheetDialogPreview() {
    BottomSheetDialog(
        showDialog = true,
        title = "My Title",
        description = "This is a description",
        onAccept = { /*TODO*/ },
        onCancel = { /*TODO*/ },
    )
}
