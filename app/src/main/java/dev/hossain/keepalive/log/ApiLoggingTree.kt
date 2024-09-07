package dev.hossain.keepalive.log

import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Custom Timber tree that sends log to an API endpoint.
 * Allows the log to be monitored remotely to analyze the app behavior.
 *
 * Create account in Airtable and create a base.
 * - https://airtable.com/signup
 *
 * Create your own auth token.
 * - https://airtable.com/create/tokens
 *
 * Get the API endpoint URL by selecting workspace from this page
 * - https://airtable.com/developers/web/api/introduction
 *
 * Create a table in Airtable with following fields:
 * - Device (Single line text)
 * - Log (Long text)
 */
class ApiLoggingTree(
    private val isEnabled: Boolean,
    private val authToken: String,
    /**
     * Airtable API endpoint URL.
     *
     * Example: https://api.airtable.com/v0/appXXXXXXXXX/Table%20Name
     *
     * Subject to rate limit:
     * https://airtable.com/developers/web/api/rate-limits
     */
    private val endpointUrl: String,
) : Timber.Tree() {
    private val client = OkHttpClient()
    private val logQueue = ConcurrentLinkedQueue<String>()
    private var flushJob: Job? = null

    companion object {
        /**
         * The API is limited to 5 requests per second per base.
         * If you exceed these rates, you will receive a 429 status code and will need to
         * wait 30 seconds before subsequent requests will succeed.
         */
        private const val MAX_LOG_COUNT_PER_SECOND = 5
    }

    init {
        if (isEnabled) {
            startFlushJob()
        }
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (!isEnabled) {
            return
        }

        val logMessage = createLogMessage(priority, tag, message, t)
        logQueue.add(logMessage)

        if (flushJob == null || flushJob?.isCancelled == true) {
            startFlushJob()
        }
    }

    private fun startFlushJob() {
        flushJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    flushLogs()
                    delay(2000L) // Delay for 2 seconds
                }
            }
    }

    private fun createLogMessage(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ): String {
        val logMessage = "Priority: ${priority.toLogType()}\nTag: $tag\nMessage: $message\nThrowable: ${t?.localizedMessage}"
        val fields =
            JSONObject().apply {
                put("Device", Build.MODEL)
                put("Log", logMessage)
            }
        val record =
            JSONObject().apply {
                put("fields", fields)
            }
        val records =
            JSONArray().apply {
                put(record)
            }
        return JSONObject().apply {
            put("records", records)
        }.toString()
    }

    private fun flushLogs() {
        var sentLogCount = 0
        while (logQueue.isNotEmpty() && sentLogCount < MAX_LOG_COUNT_PER_SECOND) {
            val log = logQueue.poll()
            if (log != null) {
                sendLogToApi(log)
                sentLogCount++
            }
        }

        if (logQueue.isEmpty()) {
            flushJob?.cancel()
        }
    }

    private fun sendLogToApi(logMessage: String) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = logMessage.toRequestBody(mediaType)
        val request =
            Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)
                .build()

        client.newCall(request).enqueue(
            object : okhttp3.Callback {
                override fun onFailure(
                    call: okhttp3.Call,
                    e: IOException,
                ) {
                    Timber.e("Failed to send log to API: ${e.localizedMessage}")
                }

                override fun onResponse(
                    call: okhttp3.Call,
                    response: okhttp3.Response,
                ) {
                    response.use { // This ensures the response body is closed
                        if (!response.isSuccessful) {
                            Timber.e("Failed to send log to API: ${response.message}")
                        }
                    }
                }
            },
        )
    }

    private fun Int.toLogType(): String {
        return when (this) {
            2 -> "VERBOSE"
            3 -> "DEBUG"
            4 -> "INFO"
            5 -> "WARN"
            6 -> "ERROR"
            7 -> "ASSERT"
            else -> "UNKNOWN"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApiLoggingTree

        return endpointUrl == other.endpointUrl
    }

    override fun hashCode(): Int {
        return endpointUrl.hashCode()
    }
}
