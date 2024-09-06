package dev.hossain.keepalive.ui.screen

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppViewModel(private val dataStore: DataStore<List<AppInfo>>) : ViewModel() {
    // LiveData for observing the list of apps
    val appList: LiveData<List<AppInfo>> = dataStore.data.map { it }.asLiveData()

    // List of selected apps
    private val _selectedApps = MutableLiveData<Set<AppInfo>>(emptySet())
    val selectedApps: LiveData<Set<AppInfo>> = _selectedApps

    fun toggleAppSelection(appInfo: AppInfo) {
        _selectedApps.value =
            _selectedApps.value?.toMutableSet()?.apply {
                if (contains(appInfo)) remove(appInfo) else add(appInfo)
            }
    }

    // Function to add new app to datastore
    fun addApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val currentList = appList.value ?: emptyList()
            dataStore.updateData { currentList + appInfo }
        }
    }

    fun removeApp(appInfo: AppInfo) {
        viewModelScope.launch {
            val currentList = appList.value ?: emptyList()
            dataStore.updateData { currentList - appInfo }
        }
    }

    /**
     * Retrieves a list of installed applications on the device, excluding system apps and the current app.
     *
     * @param context The context used to access the package manager.
     * @return A sorted list of AppInfo objects representing the installed applications.
     */
    fun getInstalledApps(context: Context): List<AppInfo> {
        val alreadyAddedApps: List<AppInfo> = appList.value ?: emptyList()
        val pm = context.packageManager
        val thisAppPackageName = context.packageName
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) &&
                    (app.packageName != thisAppPackageName) &&
                    !alreadyAddedApps.any { it.packageName == app.packageName }
            }
            .map { app -> AppInfo(app.packageName, app.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }
}
