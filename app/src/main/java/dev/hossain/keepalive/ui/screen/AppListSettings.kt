package dev.hossain.keepalive.ui.screen

import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavHostController
import dev.hossain.keepalive.data.AppDataStore
import kotlinx.coroutines.launch

/**
 * Displays the app list settings screen, allowing users to configure which apps are managed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val dataStore = AppDataStore.store(context)
    val viewModel = remember { AppViewModel(dataStore) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Apps") },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        AppListScreen(
            navController = navController,
            viewModel = viewModel,
            context = context,
            snackbarHostState = snackbarHostState,
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
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val appList by viewModel.appList.observeAsState(emptyList())
    val selectedApps by viewModel.selectedApps.observeAsState(emptySet())
    val installedApps = viewModel.getInstalledApps(context)
    val showDialog = remember { mutableStateOf(false) }
    val showDeleteDialog = remember { mutableStateOf<AppInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            if (appList.isEmpty()) {
                item {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No Apps in Watchlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your watchlist is empty. Add apps to start monitoring.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "💡 Tap the \"Add App\" button below to select apps you want to keep running.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                items(appList, key = { it.packageName }) { app ->
                    AppListItem(
                        appInfo = app,
                        isSelected = selectedApps.contains(app),
                        onAppSelected = { viewModel.toggleAppSelection(it) },
                        onDeleteRequested = { showDeleteDialog.value = it },
                        onUndoDelete = { appToRestore ->
                            viewModel.addApp(appToRestore)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        },
                        snackbarHostState = snackbarHostState,
                        coroutineScope = coroutineScope,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                showDialog.value = true
            },
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
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.addApp(it)
                    showDialog.value = false
                },
                onDismissRequest = { showDialog.value = false },
            )
        }

        // Confirmation Dialog for Delete
        showDeleteDialog.value?.let { appToDelete ->
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog.value = null
                },
                title = { Text("Remove App") },
                text = {
                    Text(
                        "Are you sure you want to remove \"${appToDelete.appName}\" from the watchlist?",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            viewModel.removeApp(appToDelete)
                            showDeleteDialog.value = null

                            // Show undo snackbar
                            coroutineScope.launch {
                                val result =
                                    snackbarHostState.showSnackbar(
                                        message = "\"${appToDelete.appName}\" removed from watchlist",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long,
                                    )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.addApp(appToDelete)
                                }
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog.value = null
                    }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

/**
 * Displays a single app item in the list with swipe-to-delete functionality and selection options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    isSelected: Boolean,
    onAppSelected: (AppInfo) -> Unit,
    onDeleteRequested: (AppInfo) -> Unit,
    onUndoDelete: (AppInfo) -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    val swipeState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        // Show delete dialog but don't confirm dismissal yet
                        onDeleteRequested(appInfo)
                        false // Don't dismiss immediately, wait for user confirmation
                    }
                    else -> false
                }
            },
        )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            // Background content when swiping - only show when actually swiping
            val dismissDirection = swipeState.dismissDirection
            if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.error)
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onError,
                        )
                        Text(
                            text = "Remove",
                            color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        content = {
            AppItemContent(
                appInfo = appInfo,
                isSelected = isSelected,
                onAppSelected = onAppSelected,
                onDeleteRequested = onDeleteRequested,
            )
        },
    )
}

/**
 * The main content of the app list item.
 */
@Composable
private fun AppItemContent(
    appInfo: AppInfo,
    isSelected: Boolean,
    onAppSelected: (AppInfo) -> Unit,
    onDeleteRequested: (AppInfo) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 4.dp else 1.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAppSelected(appInfo) }
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current
            val icon = remember { context.packageManager.getApplicationIcon(appInfo.packageName) }

            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        },
                )
            }

            // Show delete button when selected
            if (isSelected) {
                Button(
                    onClick = { onDeleteRequested(appInfo) },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
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
                    SimpleAppListItem(
                        appInfo = app,
                        onAppSelected = {
                            onAppSelected(app)
                            onDismissRequest() // Close dialog after selecting
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done")
            }
        },
    )
}

/**
 * Simple app item for the selection dialog without swipe functionality.
 */
@Composable
private fun SimpleAppListItem(
    appInfo: AppInfo,
    onAppSelected: (AppInfo) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAppSelected(appInfo) }
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val context = LocalContext.current
            val icon = remember { context.packageManager.getApplicationIcon(appInfo.packageName) }

            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}
