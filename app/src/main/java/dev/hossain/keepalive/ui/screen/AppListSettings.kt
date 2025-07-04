package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import dev.hossain.keepalive.data.AppDataStore

/**
 * Displays the app list settings screen, allowing users to configure which apps are managed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dataStore = AppDataStore.store(context)
    val viewModel = remember { AppViewModel(dataStore) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Apps") },
            )
        },
    ) { innerPadding ->
        AppListScreen(
            navController = navController,
            viewModel = viewModel,
            context = context,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
        )
    }
}

/**
 * Shows a list of apps that are kept running and allows adding or removing apps from the list.
 */
@Composable
fun AppListScreen(
    navController: NavHostController,
    viewModel: AppViewModel,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val appList by viewModel.appList.observeAsState(emptyList())
    val selectedApps by viewModel.selectedApps.observeAsState(emptySet())
    val installedApps = viewModel.getInstalledApps(context)
    val showDialog = remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "Apps that are kept running:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Text(
            text =
                "These apps will be periodically checked if they were recently run, " +
                    "if not, they will be restarted based on app configuration you choose.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        LazyColumn {
            if (appList.isEmpty()) {
                item {
                    Text(
                        text = "No apps are added to the watch list.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(appList, key = { it.packageName }) { app ->
                    AppListItem(
                        appInfo = app,
                        isSelected = selectedApps.contains(app),
                        onAppSelected = { viewModel.toggleAppSelection(it) },
                        onDelete = { viewModel.removeApp(it) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showDialog.value = true },
            modifier =
                Modifier
                    .wrapContentWidth()
                    .align(Alignment.CenterHorizontally),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Add App")
                Text("Add a new app to the watchlist.", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Done Button
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("Done")
        }

        if (showDialog.value) {
            ShowAppSelectionDialog(
                installedApps = installedApps,
                onAppSelected = {
                    viewModel.addApp(it)
                    showDialog.value = false
                },
                onDismissRequest = { showDialog.value = false },
            )
        }
    }
}

/**
 * Displays a single app item in the list with selection and delete options.
 */
@Composable
fun AppListItem(
    appInfo: AppInfo,
    isSelected: Boolean,
    onAppSelected: (AppInfo) -> Unit,
    onDelete: ((AppInfo) -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onAppSelected(appInfo) }
                .padding(16.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
    ) {
        val context = LocalContext.current
        val icon = remember { context.packageManager.getApplicationIcon(appInfo.packageName) }

        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = appInfo.appName)
            Text(text = appInfo.packageName, style = MaterialTheme.typography.bodySmall)
        }

        if (isSelected) {
            Button(onClick = { onDelete?.invoke(appInfo) }) {
                Text("Delete")
            }
        }
    }
}

/**
 * Dialog for selecting an app from the list of installed apps to add to the watchlist.
 */
@Composable
fun ShowAppSelectionDialog(
    installedApps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select an App") },
        text = {
            LazyColumn {
                items(installedApps) { app ->
                    AppListItem(
                        appInfo = app,
                        isSelected = false,
                        onAppSelected = {
                            onAppSelected(app)
                            onDismissRequest() // Close dialog after selecting
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("Done")
            }
        },
    )
}
