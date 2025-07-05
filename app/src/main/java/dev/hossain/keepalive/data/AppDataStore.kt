package dev.hossain.keepalive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dev.hossain.keepalive.ui.screen.AppInfo
import dev.hossain.keepalive.ui.screen.AppListSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Singleton object responsible for providing and managing a DataStore instance
 * dedicated to persisting a list of [AppInfo] objects.
 *
 * The DataStore is used to save the applications that the user has selected for monitoring.
 * It utilizes [AppListSerializer] to handle the serialization and deserialization of the `List<AppInfo>`.
 * The data is stored in a file named "apps_prefs.json".
 *
 * This object ensures that only one instance of the DataStore is created and used throughout the application
 * via lazy initialization in the [store] method.
 */
object AppDataStore {
    /**
     * The singleton [DataStore] instance for storing the list of [AppInfo] objects.
     * It is lazily initialized by the [store] method upon first access.
     */
    private lateinit var dataStore: DataStore<List<AppInfo>>

    /**
     * Retrieves the singleton [DataStore] instance for `List<AppInfo>`.
     *
     * If the `dataStore` has not been initialized yet, this method will call [createDataStore]
     * to create and initialize it. Subsequent calls will return the existing instance.
     * This ensures that operations are performed on a single, consistent DataStore.
     *
     * @param context The application [Context], used to create the DataStore instance if it doesn't exist.
     * @return The singleton [DataStore<List<AppInfo>>] instance.
     */
    fun store(context: Context): DataStore<List<AppInfo>> {
        synchronized(this) { // Ensure thread-safe initialization
            if (!::dataStore.isInitialized) {
                dataStore = createDataStore(context)
            }
        }
        return dataStore
    }

    /**
     * Creates and configures a new [DataStore] instance for storing a list of [AppInfo] objects.
     * This method is called internally by [store] during the first-time initialization.
     *
     * @param context The application [Context] required by [DataStoreFactory].
     * @return A new instance of [DataStore<List<AppInfo>>].
     */
    private fun createDataStore(context: Context): DataStore<List<AppInfo>> {
        return DataStoreFactory.create(
            serializer = AppListSerializer,
            produceFile = { context.dataStoreFile("apps_prefs.json") },
        )
    }

    /**
     * Provides a [Flow] that emits the current count of configured (monitored) applications.
     *
     * This method accesses the DataStore (initializing it via [store] if necessary) and maps
     * its `data` Flow (which emits `List<AppInfo>`) to a Flow that emits the size of this list.
     * This allows observers to react to changes in the number of monitored apps.
     *
     * @param context The application [Context], used to ensure DataStore initialization.
     * @return A [Flow<Int>] that emits the count of configured apps.
     */
    fun getConfiguredAppsCount(context: Context): Flow<Int> {
        return store(context).data.map { appList ->
            appList.size
        }
    }
}
