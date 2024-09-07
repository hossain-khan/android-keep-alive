package dev.hossain.keepalive.log

import android.os.Build
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Custom Timber tree that sends log to an API endpoint.
 * Allows the log to be monitored remotely to analyze the app behavior.
 */
class ApiLoggingTree(
    private val isEnabled: Boolean,
    private val authToken: String,
    private val endpointUrl: String,
) : Timber.Tree() {
    private val client = OkHttpClient()
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor()

    init {
        if (isEnabled) {
            // Schedule a task to send logs with a fixed delay
            executor.scheduleWithFixedDelay({ flushLogs() }, 1, 2, TimeUnit.SECONDS)
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
        while (logQueue.isNotEmpty()) {
            val log = logQueue.poll()
            if (log != null) {
                sendLogToApi(log)
            }
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
