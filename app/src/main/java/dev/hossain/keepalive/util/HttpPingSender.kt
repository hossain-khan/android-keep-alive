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
 * Utility class responsible for sending HTTP GET requests, typically for health check pings.
 *
 * This class uses [OkHttpClient] to perform network requests. It includes functionality
 * to construct a specific User-Agent string containing application and device information.
 * Pings are sent asynchronously on the IO dispatcher.
 *
 * @param context The application [Context], used to access package information for the User-Agent string.
 */
class HttpPingSender(private val context: Context) {
    private val client = OkHttpClient()

    /**
     * Sends an HTTP GET request to a Healthchecks.io (hc-ping.com) URL constructed with the given UUID.
     *
     * This is a convenience method that wraps [sendGenericHttpPing].
     *
     * @param pingUUID The UUID specific to the health check endpoint on hc-ping.com.
     */
    fun sendPingToDevice(pingUUID: String) {
        sendGenericHttpPing("https://hc-ping.com/$pingUUID")
    }

    /**
     * Sends an HTTP GET request to the specified URL.
     *
     * The request includes a custom User-Agent header with the following format:
     * `KA/versionName (Android OSVersion, API SDKInt, Manufacturer Model)`
     * Example: `KA/1.6 (Android 14, API 34, Google Pixel 8)`
     *
     * Network operations are performed asynchronously in a coroutine on [Dispatchers.IO].
     * Logs success or failure of the request using Timber.
     *
     * @param pingUrl The complete URL to send the GET request to.
     */
    private fun sendGenericHttpPing(pingUrl: String) {
        // Get app current version
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName

        // Construct User-Agent string
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
                        Timber.e(IOException("Unexpected HTTP code: ${response.code} for URL: $pingUrl"), "Ping failed")
                    } else {
                        Timber.d("HTTP Ping Sent Successfully to $pingUrl. Response: ${response.body?.string()}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Health check network request failed for URL: $pingUrl")
            }
        }
    }
}
