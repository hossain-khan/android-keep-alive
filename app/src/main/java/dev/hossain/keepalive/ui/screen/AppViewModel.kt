package dev.hossain.keepalive.ui.screen

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the list of applications that the user wants to monitor.
 *
 * This ViewModel interacts with a [DataStore] to persist the list of [AppInfo] objects.
 * It provides functionality to:
 * - Observe the list of monitored apps ([appList]).
 * - Manage a temporary selection of apps ([selectedApps]), typically for UI interactions like bulk removal.
 * - Add ([addApp]) and remove ([removeApp]) apps from the monitored list.
 * - Retrieve a list of all installed applications on the device ([getInstalledApps]), filtered to exclude
 *   the current app, system apps (implicitly by checking for launch intent), and apps already in the monitored list.
 *
 * @param dataStore The [DataStore] instance used for storing and retrieving the list of [AppInfo] objects.
 */
class AppViewModel(private val dataStore: DataStore<List<AppInfo>>) : ViewModel() {
    /**
     * LiveData that emits the current list of monitored applications ([AppInfo]) from the [DataStore].
     * Observers can use this to react to changes in the monitored apps list.
     */
    val appList: LiveData<List<AppInfo>> = dataStore.data.map { it }.asLiveData()

    /**
     * LiveData representing the set of currently selected [AppInfo] objects.
     * This is typically used for UI purposes, such as highlighting selected items in a list
     * or performing batch operations (e.g., removing multiple selected apps).
     * It is not persisted in DataStore directly but managed in memory by the ViewModel.
     */
    private val _selectedApps = MutableLiveData<Set<AppInfo>>(emptySet())
    val selectedApps: LiveData<Set<AppInfo>> = _selectedApps

    /**
     * Toggles the selection state of a given [AppInfo] in the [_selectedApps] set.
     * If the app is already selected, it will be deselected. If not selected, it will be added to the selection.
     *
     * @param appInfo The [AppInfo] object whose selection state is to be toggled.
     */
    fun toggleAppSelection(appInfo: AppInfo) {
        _selectedApps.value =
            _selectedApps.value?.toMutableSet()?.apply {
                if (contains(appInfo)) remove(appInfo) else add(appInfo)
            }
    }

    /**
     * Adds a specified [AppInfo] to the list of monitored applications in the [DataStore].
     * This operation is performed asynchronously within the `viewModelScope`.
     *
     * @param appInfo The [AppInfo] object representing the application to be added.
     */
    fun addApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val currentList = appList.value ?: emptyList()
            dataStore.updateData { currentList + appInfo }
        }
    }

    /**
     * Removes a specified [AppInfo] from the list of monitored applications in the [DataStore].
     * This operation is performed asynchronously within the `viewModelScope`.
     *
     * @param appInfo The [AppInfo] object representing the application to be removed.
     */
    fun removeApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val currentList = appList.value ?: emptyList()
            dataStore.updateData { currentList - appInfo }
        }
    }

    /**
     * Retrieves a list of installed applications on the device that are eligible for monitoring.
     *
     * The returned list is filtered to:
     * - Exclude the current application itself.
     * - Exclude applications that do not have a launchable activity (e.g., system services without a UI).
     * - Exclude applications that are already present in the `appList` (i.e., already being monitored).
     *
     * The resulting list is sorted alphabetically by application name.
     *
     * @param context The application [Context], used to access the [PackageManager].
     * @return A sorted list of [AppInfo] objects representing eligible installed applications.
     */
    fun getInstalledApps(context: Context): List<AppInfo> {
        val alreadyAddedApps: List<AppInfo> = appList.value ?: emptyList()
        val pm = context.packageManager
        val thisAppPackageName = context.packageName

        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                // Check if the app has a launchable activity
                val hasLaunchableActivity = pm.getLaunchIntentForPackage(app.packageName) != null

                // Allow apps with launchable activities, exclude service-only apps
                hasLaunchableActivity &&
                    (app.packageName != thisAppPackageName) &&
                    !alreadyAddedApps.any { it.packageName == app.packageName }
            }
            .map { app -> AppInfo(app.packageName, app.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }
}
