package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.hossain.keepalive.service.WatchdogService
import dev.hossain.keepalive.ui.theme.KeepAliveTheme

/**
 * Main activity that launches the WatchdogService and requests for necessary permissions.
 */
class MainActivity : ComponentActivity() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepAliveTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
        }

        requestPermissionLauncher =
            this.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                if (permissions[POST_NOTIFICATIONS] == true) {
                    // Permission granted, you can post notifications
                    Log.d(TAG, "onCreate: POST_NOTIFICATIONS Permission granted")
                } else {
                    // Permission denied, handle accordingly
                    Log.d(TAG, "onCreate: POST_NOTIFICATIONS Permission denied")
                }

                if (permissions[PACKAGE_USAGE_STATS] == true) {
                    // Permission granted, you can get package usage stats
                    Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission granted")
                } else {
                    // Permission denied, handle accordingly
                    Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission denied")
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
                        Log.d(TAG, "Overlay permission granted")
                    } else {
                        // Permission not granted, show a message to the user
                        Log.d(TAG, "Overlay permission denied")
                    }
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if ((
                    ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                ) || (
                    ContextCompat.checkSelfPermission(
                        this,
                        PACKAGE_USAGE_STATS,
                    )
                        != PackageManager.PERMISSION_GRANTED
                )
            ) {
                requestPermissionLauncher.launch(arrayOf(POST_NOTIFICATIONS, PACKAGE_USAGE_STATS))
            }
        }

        if (hasUsageStatsPermission(this)) {
            Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission granted")
        } else {
            Log.d(TAG, "onCreate: PACKAGE_USAGE_STATS Permission denied")
            requestUsageStatsPermission()
        }

        if (Settings.canDrawOverlays(this)) {
            // Permission granted, you can start the activity or service that needs this permission
        } else {
            // Permission not granted, request it
            requestOverlayPermission()
        }

        if (!isBatteryOptimizationIgnored(this)) {
            showBatteryOptimizationDialog(this)
        } else {
            Toast.makeText(
                this,
                "Battery optimization is already disabled for this app.",
                Toast.LENGTH_SHORT,
            ).show()
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

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
