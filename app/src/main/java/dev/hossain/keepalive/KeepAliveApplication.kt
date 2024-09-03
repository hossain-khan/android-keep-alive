package dev.hossain.keepalive

import android.app.Application
import dev.hossain.keepalive.log.ApiLoggingTree
import timber.log.Timber

class KeepAliveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(ApiLoggingTree("https://api.airtable.com/v0/appcUYTSp0zbLnARC/Logs"))
    }
}
