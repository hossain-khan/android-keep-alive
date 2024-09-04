package dev.hossain.keepalive

import android.app.Application
import android.os.Build
import dev.hossain.keepalive.log.ApiLoggingTree
import timber.log.Timber

class KeepAliveApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        /*
         * Custom Timber tree that sends log to an API endpoint.
         * I needed this during development to capture logs to analyze the app behavior.
         * This will allow me to ensure the functionality is working as expected.
         *
         * TO BE REMOVED BEFORE PRODUCTION.
         */
        if (Build.MODEL.contains("sdk_gphone")) {
            Timber.plant(ApiLoggingTree("https://api.airtable.com/v0/appcUYTSp0zbLnARC/LogsDebug"))
        } else {
            Timber.plant(ApiLoggingTree("https://api.airtable.com/v0/appcUYTSp0zbLnARC/Logs"))
        }
    }
}
