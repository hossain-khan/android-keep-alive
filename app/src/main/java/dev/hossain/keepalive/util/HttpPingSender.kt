package dev.hossain.keepalive.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import timber.log.Timber

/**
 * Sends HTTP ping to a given URL.
 */
class HttpPingSender(private val context: Context) {
    private val client = OkHttpClient()

    fun sendPingToDevice(pingUUID: String) {
        sendHttpPing("https://hc-ping.com/$pingUUID")
    }

    private fun sendHttpPing(pingUrl: String) {
        // Get app current version
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // Add user agent with app name, version, and device info
        // Example: `KA/1.6 (Android 14, API 34, samsung SM-S911W)`
        val userAgent =
            "KA/$versionName (Android ${android.os.Build.VERSION.RELEASE}, " +
                "API ${android.os.Build.VERSION.SDK_INT}, ${android.os.Build.MANUFACTURER} " +
                "${android.os.Build.MODEL})"

        val request =
            Request.Builder()
                .url(pingUrl)
                .header("User-Agent", userAgent)
                .build()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e(IOException("Unexpected code $response"), "Unexpected code $response")
                    } else {
                        Timber.d("Heartbeat Ping Sent: Response: " + response.body?.string())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Health check network request failed")
            }
        }
    }
}
