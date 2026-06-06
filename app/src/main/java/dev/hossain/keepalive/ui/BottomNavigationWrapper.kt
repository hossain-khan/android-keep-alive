package dev.hossain.keepalive.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.hossain.keepalive.data.PermissionType
import dev.hossain.keepalive.ui.screen.AppActivityLogScreen
import dev.hossain.keepalive.ui.screen.AppConfigScreen
import dev.hossain.keepalive.ui.screen.MainLandingScreen
import dev.hossain.keepalive.ui.screen.SettingsScreen

/**
 * Top-level composable that provides bottom navigation and routes between the main app screens.
 *
 * This composable wraps the app's navigation host in a [Scaffold] with a [BottomNavigationBar],
 * which is only displayed once all required permissions have been granted. It serves as the
 * single source of truth for edge-to-edge system bar insets; its `innerPadding` is passed down
 * to the [NavHost] so nested screens do not need to handle insets individually.
 *
 * @param navController The [NavHostController] used to navigate between screens.
 * @param context The application [Context], forwarded to screens that require it.
 * @param allPermissionsGranted Whether all required permissions have been granted. The bottom
 *   navigation bar is only shown when this is `true`.
 * @param activityResultLauncher Launcher for system-settings intents (e.g., overlay permission).
 * @param requestPermissionLauncher Launcher for standard runtime permission requests.
 * @param permissionType The next [PermissionType] to be requested, used by the home screen.
 * @param showPermissionRequestDialog Mutable state controlling the visibility of the permission dialog.
 * @param onRequestPermissions Callback invoked when the user initiates a permission request.
 * @param totalRequiredCount Total number of permissions required by the app.
 * @param grantedCount Number of permissions currently granted.
 * @param grantedPermissionsSet Set of [PermissionType] values that have already been granted.
 * @param configuredAppsCount Number of apps currently configured for monitoring; shown as a badge.
 * @param lastCheckTime Timestamp (ms) of the last completed monitoring check, or 0 if none.
 * @param serviceStartTime Timestamp (ms) when the [dev.hossain.keepalive.service.WatchdogService]
 *   was first started, or 0 if not yet started.
 */
@Composable
fun BottomNavigationWrapper(
    navController: NavHostController,
    context: Context,
    allPermissionsGranted: Boolean,
    activityResultLauncher: ActivityResultLauncher<Intent>?,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>?,
    permissionType: PermissionType,
    showPermissionRequestDialog: MutableState<Boolean>,
    onRequestPermissions: () -> Unit,
    totalRequiredCount: Int,
    grantedCount: Int,
    grantedPermissionsSet: Set<PermissionType> = emptySet(),
    configuredAppsCount: Int,
    lastCheckTime: Long = 0L,
    serviceStartTime: Long = 0L,
) {
    // Edge-to-edge: this top-level Scaffold is the single source of truth for system bar insets.
    // Its innerPadding includes status bar (top) and navigation bar (bottom) insets.
    // Passing innerPadding to the NavHost via Modifier.padding(innerPadding) ensures all
    // nested screens are automatically inset-safe without needing individual WindowInsets handling.
    Scaffold(
        bottomBar = {
            if (allPermissionsGranted) {
                BottomNavigationBar(
                    navController = navController,
                    configuredAppsCount = configuredAppsCount,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                MainLandingScreen(
                    navController = navController,
                    allPermissionsGranted = allPermissionsGranted,
                    activityResultLauncher = activityResultLauncher,
                    requestPermissionLauncher = requestPermissionLauncher,
                    permissionType = permissionType,
                    showPermissionRequestDialog = showPermissionRequestDialog,
                    onRequestPermissions = onRequestPermissions,
                    totalRequiredCount = totalRequiredCount,
                    grantedCount = grantedCount,
                    grantedPermissionsSet = grantedPermissionsSet,
                    configuredAppsCount = configuredAppsCount,
                    lastCheckTime = lastCheckTime,
                    serviceStartTime = serviceStartTime,
                )
            }
            composable(Screen.WatchDogConfig.route) {
                AppConfigScreen(
                    navController,
                    context,
                )
            }
            composable(Screen.AppWatchList.route) {
                SettingsScreen(navController)
            }
            composable(Screen.ActivityLogs.route) {
                AppActivityLogScreen(
                    navController,
                    context,
                )
            }
        }
    }
}

/**
 * Bottom navigation bar composable that renders navigation items for the main app screens.
 *
 * Shows a badge with the configured app count on the [Screen.AppWatchList] item when
 * [configuredAppsCount] is greater than zero, giving the user a quick visual indicator
 * of how many apps are being monitored.
 *
 * @param navController The [NavHostController] used to navigate when an item is selected.
 * @param configuredAppsCount The number of apps currently configured for monitoring. When
 *   greater than zero, a count badge is displayed on the "Apps" navigation item.
 */
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    configuredAppsCount: Int,
) {
    val items =
        listOf(
            Screen.Home,
            Screen.AppWatchList,
            Screen.WatchDogConfig,
            Screen.ActivityLogs,
        )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == Screen.AppWatchList && configuredAppsCount > 0) {
                                Badge(
                                    modifier =
                                        Modifier
                                            .size(20.dp)
                                            .clip(CircleShape),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ) {
                                    Text(
                                        text = configuredAppsCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                            } else {
                                null
                            }
                        },
                    ) {
                        Icon(screen.icon, contentDescription = screen.title)
                    }
                },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId) {
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
}
