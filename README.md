[![Android CI](https://github.com/hossain-khan/android-keep-alive/actions/workflows/android.yml/badge.svg)](https://github.com/hossain-khan/android-keep-alive/actions/workflows/android.yml) [![License](https://badgen.net/github/license/hossain-khan/android-keep-alive)](https://github.com/hossain-khan/android-keep-alive/blob/main/LICENSE) [![releases](https://badgen.net/github/release/hossain-khan/android-keep-alive)](https://github.com/hossain-khan/android-keep-alive/releases)

# Keep Alive 💓
A simple app to keep specific apps alive by checking if they are running. If not, they will be started for you.

> ⚠️ _DISCLAIMER: The app is mostly generated with AI (copilot, gemini, chatgpt, etc). It doesn't follow any best practices. See [CONTRIBUTING](CONTRIBUTING.md) guide for more info._

## 🛑 **STOP** 🛑 - Know before you use ✋
This app is designed with a very specific use case in mind. It is important to understand what this app does and how.

* App is designed to be **_always_** running in the foreground to watch and restart other applications.
* App is **NOT** optimized for battery and will cause high battery 🪫 drain. Ideally, this **should not** be used on your primary phone that you use daily.
    * ℹ️ I use this on a secondary phone plugged into a power cable 24/7.
* App contains some additional features like [Remote Logging](REMOTE-MONITORING.md) and [Heartbeat](REMOTE-HEARTBEAT.md) due to my personal needs. However, the app can be used without using those features.
* See the permission section below to better understand the app.

### 📚 How to use the app
1. Launch the app and accept all the required permissions.
2. Once all permission are active, you will see 2 options
    1. Configure apps that should be running
    2. Configure app settings like interval of checking for app
3. Add app that you want to be checked periodically
4. Done ✔️
    1. This app will always run in the foreground, continuously checking if your selected apps are active. If any are not, it will automatically restart them for you.  

![Keep Alive App](assets/screenshots/app-demo-screenshots.png)

## 🔐 Questionable permissions required ⚠️

Here is the list of permissions needed for the service class ([`WatchdogService`](https://github.com/hossain-khan/android-keep-alive/blob/main/app/src/main/java/dev/hossain/keepalive/service/WatchdogService.kt)). [Source: [`AndroidManifest.xml`](https://github.com/hossain-khan/android-keep-alive/blob/main/app/src/main/AndroidManifest.xml)]

Here are the permissions needed for the app along with a summary of why they are needed:

1. **`android.permission.INTERNET`**:
   - **Reason**: Required to send logs to an API endpoint and send heartbeat check. Both are optional and configurable in the app.

2. **`android.permission.FOREGROUND_SERVICE`**:
   - **Reason**: Allows the app to run a foreground service, which is necessary for the `WatchdogService` to monitor other apps and keep them alive by restarting them.

3. **`android.permission.FOREGROUND_SERVICE_SPECIAL_USE`**:
   - **Reason**: Required for special use cases of foreground services, as indicated by the `WatchdogService`.

4. **`android.permission.RECEIVE_BOOT_COMPLETED`**:
   - **Reason**: Allows the app to receive the `BOOT_COMPLETED` broadcast, enabling it to start itself and monitor the apps that are configured to keep alive.

5. **`android.permission.ACTION_LOCKED_BOOT_COMPLETED`**:
   - **Reason**: Allows the app to start service even when the device is locked, which is necessary for the `WatchdogService` to monitor other apps and keep them alive by restarting them.

6. **`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`**:
   - **Reason**: Needed to request the user to exclude the app from battery optimizations, ensuring the app and it's `WatchdogService` can run continuously without being restricted by the system.

7. **`android.permission.POST_NOTIFICATIONS`**:
   - **Reason**: Allows the app to post notifications, which is essential for notifying users about ongoing watchdog activity.

8. **`android.permission.QUERY_ALL_PACKAGES`**:
   - **Reason**: Required to query and interact with all installed packages, which is necessary for the app's functionality to monitor and select apps in the app settings.

9. **`android.permission.PACKAGE_USAGE_STATS`**:
   - **Reason**: Allows the app to access usage statistics, which is necessary for knowing if specific apps have been recently used.

10. **`android.permission.SYSTEM_ALERT_WINDOW`**:
    - **Reason**: Required to draw overlays on top of other apps, which is necessary for certain UI elements or notifications that need to be displayed over other apps. And also start other apps from the background service.

Most of the permissions listed above are discouraged and or restricted. Please be sure to use the app with caution.
