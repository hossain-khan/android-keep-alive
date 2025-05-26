package dev.hossain.keepalive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dev.hossain.keepalive.ui.screen.AppInfo
import dev.hossain.keepalive.ui.screen.AppListSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AppDataStore {
    lateinit var dataStore: DataStore<List<AppInfo>>

    fun store(context: Context): DataStore<List<AppInfo>> {
        if (!::dataStore.isInitialized) {
            dataStore = createDataStore(context)
        }

        return dataStore
    }

    private fun createDataStore(context: Context): DataStore<List<AppInfo>> {
        return DataStoreFactory.create(
            serializer = AppListSerializer,
            produceFile = { context.dataStoreFile("apps_prefs.json") },
        )
    }

    /**
     * Get a Flow that will emit the count of configured apps.
     *
     * @param context The context to initialize the DataStore if not already initialized
     * @return A Flow emitting the count of configured apps
     */
    fun getConfiguredAppsCount(context: Context): Flow<Int> {
        return store(context).data.map { appList ->
            appList.size
        }
    }
}
