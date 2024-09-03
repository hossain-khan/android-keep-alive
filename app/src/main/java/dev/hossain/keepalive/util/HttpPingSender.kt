package dev.hossain.keepalive.util

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import timber.log.Timber

/**
 * Sends HTTP ping to a given URL.
 */
class HttpPingSender(private val context: Context) {
    private val client = OkHttpClient()

    fun sendPingToDevice() {
        val deviceModel = android.os.Build.MODEL
        AppConfig.phoneToUrlMap[deviceModel]?.let { pingUrl ->
            Timber.d("sendPingToDevice: Pinging device $deviceModel with URL $pingUrl")
            sendHttpPing(pingUrl)
        } ?: run {
            Timber.e("sendPingToDevice: No URL found for device $deviceModel - Not supported.")
        }
    }

    private fun sendHttpPing(pingUrl: String) {
        // Get app current version
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // Add user agent with app name, version, and device info
        val userAgent =
            "KA/$versionName (Android ${android.os.Build.VERSION.RELEASE}, " +
                "API ${android.os.Build.VERSION.SDK_INT}, ${android.os.Build.MANUFACTURER} " +
                "${android.os.Build.MODEL})"

        val request =
            Request.Builder()
                .url(pingUrl)
                .header("User-Agent", userAgent)
                .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.e(IOException("Unexpected code $response"), "Unexpected code $response")
            } else {
                Timber.d("Heartbeat Ping Sent: Response: " + response.body?.string())
            }
        }
    }
}
