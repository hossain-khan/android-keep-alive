package dev.hossain.keepalive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dev.hossain.keepalive.ui.screen.AppInfo
import dev.hossain.keepalive.ui.screen.AppListSerializer

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
}
