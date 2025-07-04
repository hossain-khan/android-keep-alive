package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.data.PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_PACKAGE_USAGE_STATS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_POST_NOTIFICATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY
import dev.hossain.keepalive.ui.Screen
import dev.hossain.keepalive.ui.screen.AppActivityLogScreen
import dev.hossain.keepalive.ui.screen.AppConfigScreen
import dev.hossain.keepalive.ui.screen.MainLandingScreen
import dev.hossain.keepalive.ui.screen.MainViewModel
import dev.hossain.keepalive.ui.screen.SettingsScreen
import dev.hossain.keepalive.ui.theme.KeepAliveTheme
import dev.hossain.keepalive.util.ServiceManager
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

        setContent {
            KeepAliveTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    showPermissionRequestDialog = remember { mutableStateOf(false) }
                    nextPermissionType = remember { mutableStateOf(PERMISSION_POST_NOTIFICATIONS) }
                    val grantedPermissionCount by mainViewModel.totalApprovedPermissions.observeAsState(0)
                    val allPermissionsGranted by mainViewModel.allPermissionsGranted.observeAsState(false)
                    val navController: NavHostController = rememberNavController()

                    // Get the configured apps count from AppDataStore
                    val configuredAppsCount by AppDataStore.getConfiguredAppsCount(applicationContext)
                        .collectAsState(initial = 0)

                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            MainLandingScreen(
                                navController = navController,
                                allPermissionsGranted = allPermissionsGranted,
                                activityResultLauncher = activityResultLauncher,
                                requestPermissionLauncher = requestPermissionLauncher,
                                permissionType = nextPermissionType.value,
                                showPermissionRequestDialog = showPermissionRequestDialog,
                                onRequestPermissions = { requestNextRequiredPermission() },
                                totalRequiredCount = mainViewModel.totalPermissionRequired,
                                grantedCount = grantedPermissionCount,
                                configuredAppsCount = configuredAppsCount,
                            )
                        }
                        composable(Screen.AppConfigs.route) {
                            AppConfigScreen(
                                navController,
                                applicationContext,
                            )
                        }
                        composable(Screen.AppSettings.route) { SettingsScreen(navController) }
                        composable(Screen.ActivityLogs.route) {
                            AppActivityLogScreen(
                                navController,
                                applicationContext,
                            )
                        }
                    }
                }
            }
        }

        // Start the WatchdogService - this is required to monitor other apps and keep them alive.
        ServiceManager.startWatchdogService(this)

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
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionGrantedStatus()
    }
}
