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
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

                    if (allPermissionsGranted) {
                        MainAppScreen(
                            navController = navController,
                            mainViewModel = mainViewModel,
                            configuredAppsCount = configuredAppsCount,
                        )
                    } else {
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

@Composable
fun MainAppScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    configuredAppsCount: Int,
) {
    val items =
        listOf(
            Screen.Home,
            Screen.AppConfigs,
            Screen.AppSettings,
            Screen.ActivityLogs,
        )
    Scaffold(
        bottomBar = {
            BottomAppBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon(),
                                contentDescription = null,
                            )
                        },
                        label = { Text(screen.name) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Screen.Home.route) {
                // TODO: Replace with actual home screen content.
                // For now, using MainLandingScreen without permission UI.
                MainLandingScreen(
                    navController = navController,
                    allPermissionsGranted = true, // Assuming all permissions are granted here
                    activityResultLauncher = null,
                    requestPermissionLauncher = null,
                    permissionType = PERMISSION_POST_NOTIFICATIONS, // Dummy value
                    showPermissionRequestDialog = remember { mutableStateOf(false) }, // Dummy value
                    onRequestPermissions = {},
                    totalRequiredCount = mainViewModel.totalPermissionRequired,
                    grantedCount = mainViewModel.totalPermissionRequired, // Assuming all granted
                    configuredAppsCount = configuredAppsCount,
                )
            }
            composable(Screen.AppConfigs.route) {
                AppConfigScreen(
                    navController,
                    navController.context,
                )
            }
            composable(Screen.AppSettings.route) { SettingsScreen(navController) }
            composable(Screen.ActivityLogs.route) {
                AppActivityLogScreen(
                    navController,
                    navController.context,
                )
            }
        }
    }
}

/**
 * Returns the appropriate icon for each screen.
 */
fun Screen.icon(): ImageVector {
    return when (this) {
        Screen.Home -> Icons.Filled.Home
        Screen.AppConfigs -> Icons.Filled.List // Corrected Icon
        Screen.AppSettings -> Icons.Filled.Settings // Corrected Icon
        Screen.ActivityLogs -> Icons.Filled.Warning
    }
}
