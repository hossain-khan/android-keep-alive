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

/**
 * Custom Timber tree that sends log to an API endpoint.
 * I needed this during development to capture logs to analyze the app behavior.
 * This will allow me to ensure the functionality is working as expected.
 *
 * TO BE REMOVED BEFORE PRODUCTION.
 */
class ApiLoggingTree(private val endpointUrl: String) : Timber.Tree() {
    private val client = OkHttpClient()

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        val logMessage = createLogMessage(priority, tag, message, t)
        sendLogToApi(logMessage)
    }

    // NOTE: No PII (Personally Identifiable Information) is logged.
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

    private fun sendLogToApi(logMessage: String) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = logMessage.toRequestBody(mediaType)
        val request =
            Request.Builder()
                .url(endpointUrl)
                // WARNING: Exposed token that is used for development only. Will be revoked before production.
                .addHeader("Authorization", "Bearer patUZtdmJOhvqUkkt.146538e55ff830103df98000dec37899cf3cdede09a2e9bbb3c4214048351702")
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
}
