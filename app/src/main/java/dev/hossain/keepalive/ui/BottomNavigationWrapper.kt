package dev.hossain.keepalive.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
) {
    Scaffold(
        bottomBar = {
            if (allPermissionsGranted) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
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
                )
            }
            composable(Screen.AppConfigs.route) {
                AppConfigScreen(
                    navController,
                    context,
                )
            }
            composable(Screen.AppSettings.route) {
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
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.AppSettings,
        Screen.AppConfigs,
        Screen.ActivityLogs
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
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
                }
            )
        }
    }
}