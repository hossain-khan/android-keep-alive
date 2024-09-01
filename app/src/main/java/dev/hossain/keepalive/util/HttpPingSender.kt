package dev.hossain.keepalive.util

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

/**
 * Sends HTTP ping to a given URL.
 */
class HttpPingSender(private val context: Context) {
    companion object {
        private const val TAG = "HttpPingSender"
    }

    private val client = OkHttpClient()

    fun sendHttpPing(pingUrl: String) {
        // Get app current version
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // Add user agent with app name, version, and device info
        val userAgent =
            "KA/${versionName} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT} ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL})"

        val request = Request.Builder()
            .url(pingUrl)
            .header("User-Agent", userAgent)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Unexpected code $response", IOException("Unexpected code $response"))
            } else {
                Log.d(TAG, "sendHttpPing: Response: ${response.body!!.string()}")
            }
        }
    }
}