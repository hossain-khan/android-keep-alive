package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.data.PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_PACKAGE_USAGE_STATS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_POST_NOTIFICATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.ui.theme.KeepAliveTheme
import dev.hossain.keepalive.util.AppPermissions
import timber.log.Timber

/**
 * Main activity that launches the WatchdogService and requests for necessary permissions.
 */
class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var showPermissionRequestDialog: MutableState<Boolean>
    private lateinit var nextPermissionType: MutableState<PermissionType>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepAliveTheme {
                showPermissionRequestDialog = remember { mutableStateOf(false) }
                nextPermissionType =
                    remember { mutableStateOf(PERMISSION_POST_NOTIFICATIONS) }

                val allPermissionsGranted: Boolean by mainViewModel.allPermissionsGranted.observeAsState(
                    false,
                )

                MainLandingScreen(
                    allPermissionsGranted = allPermissionsGranted,
                    activityResultLauncher = activityResultLauncher,
                    requestPermissionLauncher = requestPermissionLauncher,
                    permissionType = nextPermissionType.value,
                    showPermissionRequestDialog = showPermissionRequestDialog,
                    onRequestPermissions = { requestNextRequiredPermission() },
                )
            }
        }

        // Start the WatchdogService - this is required to monitor other apps and keep them alive.
        startWatchdogService()

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
    }

    private fun updatePermissionGrantedStatus(): Boolean {
        mainViewModel.checkAllPermissions(this)

        return mainViewModel.requiredPermissionRemaining.isEmpty()
    }

    private fun requestNextRequiredPermission() {
        if (updatePermissionGrantedStatus()) {
            Timber.d("requestNextRequiredPermission: All required permissions granted.")
            Toast.makeText(this, "All required permissions granted.", Toast.LENGTH_SHORT).show()
            return
        }

        mainViewModel.requiredPermissionRemaining.first().let { permissionType: PermissionType ->
            when (permissionType) {
                PERMISSION_POST_NOTIFICATIONS -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_POST_NOTIFICATIONS permission")
                    nextPermissionType.value = PERMISSION_POST_NOTIFICATIONS
                    showPermissionRequestDialog.value = true
                }

                PERMISSION_PACKAGE_USAGE_STATS -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_PACKAGE_USAGE_STATS permission")
                    nextPermissionType.value = PERMISSION_PACKAGE_USAGE_STATS
                    showPermissionRequestDialog.value = true
                }

                PERMISSION_SYSTEM_APPLICATION_OVERLAY -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_SYSTEM_APPLICATION_OVERLAY permission")
                    nextPermissionType.value = PERMISSION_SYSTEM_APPLICATION_OVERLAY
                    showPermissionRequestDialog.value = true
                }

                PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS permission")
                    nextPermissionType.value = PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS
                    showPermissionRequestDialog.value = true
                }

                PermissionType.PERMISSION_FOREGROUND_SERVICE -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_FOREGROUND_SERVICE permission")
                    nextPermissionType.value = PermissionType.PERMISSION_FOREGROUND_SERVICE
                    showPermissionRequestDialog.value = true
                }

                PermissionType.PERMISSION_FOREGROUND_SERVICE_SPECIAL_USE -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_FOREGROUND_SERVICE_SPECIAL_USE permission")
                    nextPermissionType.value =
                        PermissionType.PERMISSION_FOREGROUND_SERVICE_SPECIAL_USE
                    showPermissionRequestDialog.value = true
                }

                PermissionType.PERMISSION_RECEIVE_BOOT_COMPLETED -> {
                    Timber.d("requestNextRequiredPermission: Requesting PERMISSION_RECEIVE_BOOT_COMPLETED permission")
                    nextPermissionType.value = PermissionType.PERMISSION_RECEIVE_BOOT_COMPLETED
                    showPermissionRequestDialog.value = true
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updatePermissionGrantedStatus()
    }

    private fun startWatchdogService() {
        val serviceIntent = Intent(this, WatchdogService::class.java)
        startService(serviceIntent)
    }
}

@Composable
fun MainLandingScreen(
    modifier: Modifier = Modifier,
    activityResultLauncher: ActivityResultLauncher<Intent>?,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
    allPermissionsGranted: Boolean = false,
    permissionType: PermissionType,
    showPermissionRequestDialog: MutableState<Boolean>,
    onRequestPermissions: () -> Unit,
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
                text = "App that keeps photos and sync apps alive.",
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
                        onClick = { onRequestPermissions() },
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 32.dp),
                    ) {
                        Text("Grant Permissions")
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

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainLandingScreenPreview() {
    KeepAliveTheme {
        MainLandingScreen(
            allPermissionsGranted = true,
            activityResultLauncher = null,
            requestPermissionLauncher = null,
            permissionType = PERMISSION_POST_NOTIFICATIONS,
            showPermissionRequestDialog = mutableStateOf(false),
            onRequestPermissions = {},
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun MainLandingScreenPreviewWithoutButton() {
    KeepAliveTheme {
        MainLandingScreen(
            allPermissionsGranted = false,
            activityResultLauncher = null,
            requestPermissionLauncher = null,
            permissionType = PERMISSION_POST_NOTIFICATIONS,
            showPermissionRequestDialog = mutableStateOf(false),
            onRequestPermissions = {},
        )
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

@Composable
fun PermissionDialogs(
    context: Context,
    activityResultLauncher: ActivityResultLauncher<Intent>?,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
    permissionType: PermissionType,
    showDialog: MutableState<Boolean>,
) {
    val (title, description) =
        when (permissionType) {
            PERMISSION_POST_NOTIFICATIONS -> "Post Notifications" to "Please grant the notification permission."
            PERMISSION_PACKAGE_USAGE_STATS -> "Usage Stats" to "Please grant the usage stats permission."
            PERMISSION_SYSTEM_APPLICATION_OVERLAY -> "Overlay Permission" to "Please grant the overlay permission."
            PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS -> "Battery Optimization" to "Please exclude this app from battery optimization."
            else -> "X" to "Y"
        }

    BottomSheetDialog(
        showDialog = showDialog.value,
        title = title,
        description = description,
        onAccept = {
            Timber.d("onAccept: for $permissionType")
            showDialog.value = false
            requestPermission(
                context = context,
                activityResultLauncher = activityResultLauncher,
                requestPermissionLauncher = requestPermissionLauncher,
                permissionType = permissionType,
            )
        },
        onCancel = {
            Timber.d("onCancel: for $permissionType")
            showDialog.value = false
        },
    )
}

@SuppressLint("NewApi")
fun requestPermission(
    context: Context,
    activityResultLauncher: ActivityResultLauncher<Intent>?,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
    permissionType: PermissionType,
) {
    Timber.d("requestPermission: for $permissionType")
    when (permissionType) {
        PERMISSION_POST_NOTIFICATIONS -> {
            // Request for notification permission
            AppPermissions.requestPostNotificationPermission(requestPermissionLauncher!!)
        }

        PERMISSION_PACKAGE_USAGE_STATS -> {
            // Request for usage stats permission
            AppPermissions.requestUsageStatsPermission(context)
        }

        PERMISSION_SYSTEM_APPLICATION_OVERLAY -> {
            // Request for overlay permission
            activityResultLauncher?.let {
                AppPermissions.requestOverlayPermission(context, it)
            }
        }

        PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS -> {
            // Request for battery optimization exclusion
            AppPermissions.requestBatteryOptimizationExclusion(context)
        }

        else -> {
            // Do nothing
        }
    }
}
