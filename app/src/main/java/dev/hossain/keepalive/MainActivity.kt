package dev.hossain.keepalive

import android.Manifest.permission.PACKAGE_USAGE_STATS
import android.Manifest.permission.POST_NOTIFICATIONS
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dev.hossain.keepalive.data.AppDataStore
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.data.PermissionType.PERMISSION_IGNORE_BATTERY_OPTIMIZATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_PACKAGE_USAGE_STATS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_POST_NOTIFICATIONS
import dev.hossain.keepalive.data.PermissionType.PERMISSION_SYSTEM_APPLICATION_OVERLAY
import dev.hossain.keepalive.data.SettingsRepository
import dev.hossain.keepalive.ui.BottomNavigationWrapper
import dev.hossain.keepalive.ui.screen.MainViewModel
import dev.hossain.keepalive.ui.theme.KeepAliveTheme
import dev.hossain.keepalive.util.ServiceManager
import timber.log.Timber

/**
 * Main activity for the KeepAlive application.
 *
 * This activity is responsible for:
 * - Setting up the user interface.
 * - Managing and requesting necessary runtime permissions.
 * - Initializing and starting the [WatchdogService] to monitor applications.
 * - Handling results from permission requests and system settings changes.
 */
class MainActivity : ComponentActivity() {
    /**
     * ActivityResultLauncher for requesting multiple permissions.
     * This is used to request standard Android runtime permissions like [android.Manifest.permission.POST_NOTIFICATIONS]
     * and [android.Manifest.permission.PACKAGE_USAGE_STATS].
     */
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    /**
     * ActivityResultLauncher for starting an activity for a result.
     * This is primarily used for permissions that require navigating to system settings,
     * such as the overlay permission ([android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION])
     * or battery optimization exclusion.
     */
    private lateinit var activityResultLauncherForSystemSettings: ActivityResultLauncher<Intent>

    private val mainViewModel: MainViewModel by viewModels()

    /** State variable to control the visibility of the permission request dialog. */
    private lateinit var showPermissionRequestDialog: MutableState<Boolean>

    /** State variable to store the next permission type to be requested. */
    private lateinit var nextPermissionType: MutableState<PermissionType>

    /**
     * Called when the activity is first created.
     *
     * This function sets up the UI, initializes permission launchers, and starts the WatchdogService.
     * It also observes LiveData for permission status and configured app count.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for proper insets handling
        enableEdgeToEdge()

        setContent {
            // Observe theme preference
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = dev.hossain.keepalive.ui.theme.ThemeMode.SYSTEM)

            KeepAliveTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    showPermissionRequestDialog = remember { mutableStateOf(false) }
                    nextPermissionType = remember { mutableStateOf(PERMISSION_POST_NOTIFICATIONS) }
                    val grantedPermissionCount by mainViewModel.totalApprovedPermissions.observeAsState(0)
                    val allPermissionsGranted by mainViewModel.allPermissionsGranted.observeAsState(false)
                    val navController = rememberNavController()

                    // Get the configured apps count from AppDataStore
                    val configuredAppsCount by AppDataStore.getConfiguredAppsCount(applicationContext)
                        .collectAsState(initial = 0)

                    // Get the last check time and service start time
                    val lastCheckTime by settingsRepository.lastCheckTimeFlow.collectAsState(initial = 0L)
                    val serviceStartTime by settingsRepository.serviceStartTimeFlow.collectAsState(initial = 0L)

                    BottomNavigationWrapper(
                        navController = navController,
                        context = applicationContext,
                        allPermissionsGranted = allPermissionsGranted,
                        activityResultLauncher = activityResultLauncherForSystemSettings,
                        requestPermissionLauncher = permissionLauncher,
                        permissionType = nextPermissionType.value,
                        showPermissionRequestDialog = showPermissionRequestDialog,
                        onRequestPermissions = { requestNextRequiredPermission() },
                        totalRequiredCount = mainViewModel.totalPermissionRequired,
                        grantedCount = grantedPermissionCount,
                        configuredAppsCount = configuredAppsCount,
                        lastCheckTime = lastCheckTime,
                        serviceStartTime = serviceStartTime,
                    )
                }
            }
        }

        // Start the WatchdogService - this is required to monitor other apps and keep them alive.
        ServiceManager.startWatchdogService(this)

        permissionLauncher =
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
        activityResultLauncherForSystemSettings =
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

    /**
     * Updates the status of all required permissions by checking them and updates the ViewModel.
     *
     * @return `true` if all required permissions are granted, `false` otherwise.
     */
    private fun updatePermissionGrantedStatus(): Boolean {
        mainViewModel.checkAllPermissions(this)

        return mainViewModel.requiredPermissionRemaining.isEmpty()
    }

    /**
     * Requests the next required permission from the user.
     *
     * It first checks if all permissions are already granted. If not, it identifies the next
     * pending permission and triggers a dialog to request it from the user.
     */
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

    /**
     * Called when the activity will start interacting with the user.
     * At this point your activity is at the top of the activity stack, with user input going to it.
     * It also updates the permission status.
     */
    override fun onResume() {
        super.onResume()
        updatePermissionGrantedStatus()
    }
}
