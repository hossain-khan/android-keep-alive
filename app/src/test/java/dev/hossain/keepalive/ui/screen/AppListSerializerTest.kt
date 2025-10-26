package dev.hossain.keepalive.ui.screen

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Unit tests for AppListSerializer to verify proper serialization and deserialization
 * of app watch list data using JSON format.
 */
class AppListSerializerTest {
    @Test
    fun `defaultValue returns empty list`() {
        assertEquals(emptyList<AppInfo>(), AppListSerializer.defaultValue)
    }

    @Test
    fun `writeTo and readFrom with empty list`() =
        runTest {
            val emptyList = emptyList<AppInfo>()
            val outputStream = ByteArrayOutputStream()

            // Serialize empty list
            AppListSerializer.writeTo(emptyList, outputStream)

            // Deserialize back
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(emptyList, result)
        }

    @Test
    fun `writeTo and readFrom with single app`() =
        runTest {
            val singleApp = listOf(AppInfo("com.example.app", "Example App"))
            val outputStream = ByteArrayOutputStream()

            // Serialize single app
            AppListSerializer.writeTo(singleApp, outputStream)

            // Deserialize back
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(singleApp, result)
            assertEquals("com.example.app", result[0].packageName)
            assertEquals("Example App", result[0].appName)
        }

    @Test
    fun `writeTo and readFrom with multiple apps`() =
        runTest {
            val multipleApps =
                listOf(
                    AppInfo("com.example.app1", "First App"),
                    AppInfo("com.example.app2", "Second App"),
                    AppInfo("com.google.android.gm", "Gmail"),
                    AppInfo("com.spotify.music", "Spotify"),
                )
            val outputStream = ByteArrayOutputStream()

            // Serialize multiple apps
            AppListSerializer.writeTo(multipleApps, outputStream)

            // Deserialize back
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(multipleApps, result)
            assertEquals(4, result.size)
            assertEquals("com.example.app1", result[0].packageName)
            assertEquals("Gmail", result[2].appName)
        }

    @Test
    fun `writeTo and readFrom with special characters in app names`() =
        runTest {
            val appsWithSpecialChars =
                listOf(
                    AppInfo("com.example.app", "App with émojis 🚀"),
                    AppInfo("com.test.quotes", "App with \"quotes\""),
                    AppInfo("com.test.unicode", "App with ñoño & símbolos"),
                )
            val outputStream = ByteArrayOutputStream()

            // Serialize apps with special characters
            AppListSerializer.writeTo(appsWithSpecialChars, outputStream)

            // Deserialize back
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(appsWithSpecialChars, result)
            assertEquals("App with émojis 🚀", result[0].appName)
            assertEquals("App with \"quotes\"", result[1].appName)
            assertEquals("App with ñoño & símbolos", result[2].appName)
        }

    @Test
    fun `readFrom with corrupted data returns empty list`() =
        runTest {
            val corruptedData = "{ invalid json data }".toByteArray()
            val inputStream = ByteArrayInputStream(corruptedData)

            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(emptyList<AppInfo>(), result)
        }

    @Test
    fun `readFrom with empty input stream returns empty list`() =
        runTest {
            val inputStream = ByteArrayInputStream(byteArrayOf())

            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(emptyList<AppInfo>(), result)
        }

    @Test
    fun `readFrom with partial JSON returns empty list`() =
        runTest {
            val partialJson = "[{\"packageName\":\"com.example\",\"appName\":".toByteArray()
            val inputStream = ByteArrayInputStream(partialJson)

            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(emptyList<AppInfo>(), result)
        }

    @Test
    fun `serialized data format is valid JSON`() =
        runTest {
            val apps =
                listOf(
                    AppInfo("com.example.app", "Example App"),
                    AppInfo("com.test.app", "Test App"),
                )
            val outputStream = ByteArrayOutputStream()

            AppListSerializer.writeTo(apps, outputStream)
            val jsonString = outputStream.toString("UTF-8")

            // Verify it's valid JSON by checking basic structure
            assertTrue("JSON should start with [", jsonString.startsWith("["))
            assertTrue("JSON should end with ]", jsonString.endsWith("]"))
            assertTrue("JSON should contain packageName", jsonString.contains("packageName"))
            assertTrue("JSON should contain appName", jsonString.contains("appName"))
        }

    @Test
    fun `round trip serialization preserves data integrity`() =
        runTest {
            val originalApps =
                listOf(
                    AppInfo("com.whatsapp", "WhatsApp"),
                    AppInfo("com.instagram.android", "Instagram"),
                    AppInfo("com.facebook.katana", "Facebook"),
                    AppInfo("org.mozilla.firefox", "Firefox Browser"),
                )

            // Multiple round trips
            var currentApps = originalApps
            repeat(5) {
                val outputStream = ByteArrayOutputStream()
                AppListSerializer.writeTo(currentApps, outputStream)

                val inputStream = ByteArrayInputStream(outputStream.toByteArray())
                currentApps = AppListSerializer.readFrom(inputStream)
            }

            assertEquals("Data should remain intact after multiple serialization cycles", originalApps, currentApps)
        }

    @Test
    fun `writeTo with apps containing long package names and app names`() =
        runTest {
            val longPackageName = "com.very.long.package.name.that.exceeds.normal.length.limits.example.application"
            val longAppName = "This is a very long application name that might contain multiple words and special characters !@#$%^&*()"

            val appsWithLongNames = listOf(AppInfo(longPackageName, longAppName))
            val outputStream = ByteArrayOutputStream()

            AppListSerializer.writeTo(appsWithLongNames, outputStream)

            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            val result = AppListSerializer.readFrom(inputStream)

            assertEquals(appsWithLongNames, result)
            assertEquals(longPackageName, result[0].packageName)
            assertEquals(longAppName, result[0].appName)
        }
}
