package dev.hossain.keepalive.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
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
    configuredAppsCount: Int,
    lastCheckTime: Long = 0L,
    serviceStartTime: Long = 0L,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
    ) {
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
