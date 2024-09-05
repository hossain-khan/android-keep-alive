package dev.hossain.keepalive.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.navigation.NavHostController

fun appDataStore(context: Context): DataStore<List<AppInfo>> {
    return DataStoreFactory.create(
        serializer = AppListSerializer,
        produceFile = { context.dataStoreFile("apps_prefs.json") },
    )
}

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dataStore = appDataStore(context)
    val viewModel = remember { AppViewModel(dataStore) }

    AppListScreen(navController, viewModel, context)
}

@Composable
fun AppListScreen(
    navController: NavHostController,
    viewModel: AppViewModel,
    context: Context,
) {
    val appList by viewModel.appList.observeAsState(emptyList())
    val selectedApps by viewModel.selectedApps.observeAsState(emptySet())
    val installedApps = viewModel.getInstalledApps(context)
    val showDialog = remember { mutableStateOf(false) }

    Column {
        // List of Apps (from datastore)
        LazyColumn {
            items(appList) { app ->
                AppListItem(
                    appInfo = app,
                    isSelected = selectedApps.contains(app),
                    onAppSelected = { viewModel.toggleAppSelection(it) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to add app from installed apps
        Button(onClick = {
            // Logic to show a dialog to select an app from installed apps
            showDialog.value = true
        }) {
            Text("Add App")
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

@Composable
fun AppListItem(
    appInfo: AppInfo,
    isSelected: Boolean,
    onAppSelected: (AppInfo) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onAppSelected(appInfo) }
                .padding(16.dp)
                .background(if (isSelected) Color.LightGray else Color.Transparent),
    ) {
        // Load app icon using package id
        val context = LocalContext.current
        val icon = remember { context.packageManager.getApplicationIcon(appInfo.packageName) }

        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = appInfo.appName)
            Text(text = appInfo.packageName, style = MaterialTheme.typography.bodySmall)
        }
    }
}

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
