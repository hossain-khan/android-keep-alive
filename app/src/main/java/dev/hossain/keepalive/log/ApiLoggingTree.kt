package dev.hossain.keepalive.log

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
 *
 * See additional guide on the [GitHub repository](https://github.com/hossain-khan/android-keep-alive/blob/main/REMOTE-MONITORING.md).
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
    private val logQueue = ConcurrentLinkedQueue<LogMessage>()
    private var flushJob: Job? = null
    private var logSequence = 1

    companion object {
        /**
         * The API is limited to 5 requests per second per base.
         * If you exceed these rates, you will receive a 429 status code and will need to
         * wait 30 seconds before subsequent requests will succeed.
         */
        private const val MAX_LOG_COUNT_PER_SECOND = 5

        /**
         * The API is limited to 10 records per request.
         * - https://airtable.com/developers/web/api/create-records
         */
        private const val MAX_RECORDS_PER_REQUEST = 10
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

        logQueue.add(LogMessage(priority, tag, message, t, logSequence++))

        if (flushJob == null || flushJob?.isCancelled == true) {
            startFlushJob()
        }
    }

    private fun startFlushJob() {
        flushJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    flushLogs()

                    // https://airtable.com/developers/web/api/rate-limits
                    delay(1_100L)
                }
            }
    }

    /**
     * Creates log message in JSON format based on following specification:
     * - https://airtable.com/developers/web/api/create-records
     *
     * Your request body should include an array of up to 10 record objects.
     * - https://airtable.com/developers/web/api/create-records
     */
    private fun createLogMessage(logs: List<LogMessage>): String? {
        if (logs.isEmpty()) {
            return null
        }

        val records = JSONArray().apply { logs.forEach { put(it.toLogRecord()) } }
        return JSONObject().apply {
            put("records", records)
        }.toString()
    }

    private fun getMaximumAllowedLogs(): List<LogMessage> {
        val logs = mutableListOf<LogMessage>()
        while (logQueue.isNotEmpty() && logs.size < MAX_RECORDS_PER_REQUEST) {
            val log = logQueue.poll()
            if (log != null) {
                logs.add(log)
            }
        }
        return logs
    }

    private suspend fun flushLogs() {
        var sentLogCount = 0

        while (sentLogCount < MAX_LOG_COUNT_PER_SECOND) {
            val jsonPayload = createLogMessage(getMaximumAllowedLogs())
            if (jsonPayload != null) {
                sendLogToApi(jsonPayload)
                sentLogCount++

                // This delay is added to ensure the order of log is maintained.
                // However, there is no guarantee that the log will be sent in order.
                delay(100L)
            }
        }

        if (logQueue.isEmpty()) {
            flushJob?.cancel()
        }
    }

    private fun sendLogToApi(logPayloadJson: String) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = logPayloadJson.toRequestBody(mediaType)
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
                            Timber.e(
                                "Log is rejected: HTTP code: ${response.code}, " +
                                    "message: ${response.message}, body: ${response.body?.string()}",
                            )
                        }
                    }
                }
            },
        )
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
