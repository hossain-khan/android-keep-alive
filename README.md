# Keep Alive 💓
A simple app to keep specific apps alive by checking if they are running. If not, they will be launched.

## Apps Kept Alive 🩺
Currently, the following apps are hard-coded to meet specific needs to upload files via Google Photos.

1. Google Photos
2. Syncthing


## 🔐 Questionable permissions required ⚠️

Here is the list of permissions needed for the service class ([`WatchdogService`](https://github.com/hossain-khan/android-keep-alive/blob/main/app/src/main/java/dev/hossain/keepalive/service/WatchdogService.kt)). [Source: [Stackoverflow](https://android.stackexchange.com/a/258241/5002)]

1. `FOREGROUND_SERVICE`: Allows the app to run foreground services. This is required for the `WatchdogService` to keep running in the foreground and monitor other apps.  
1. `FOREGROUND_SERVICE_SPECIAL_USE`: Allows the app to use special foreground services. This is used for services that have special use cases, such as the `WatchdogService`.  
1. `RECEIVE_BOOT_COMPLETED`: Allows the app to receive the BOOT_COMPLETED broadcast. This is necessary to restart the `WatchdogService` when the device boots up.  
1. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Allows the app to request the user to ignore battery optimizations. This is required to ensure the app can run in the background without being killed by the system.  
1. `POST_NOTIFICATIONS`: Allows the app to post notifications. This is necessary to notify the user about the status of monitored apps.  
1. `PACKAGE_USAGE_STATS`: Allows the app to access usage stats of other apps. This is required to check if specific apps are running.  
1. `SYSTEM_ALERT_WINDOW`: Allows the app to draw overlays on top of other apps. This is necessary for the app to start activities from the background on newer versions of Android.
1. `ACTION_BOOT_COMPLETED`: Allows the app to restart service as soon as the phone is restarted and is back online. 

Most of the permissions listed above are discouraged and or restricted. Please be sure to use the app with caution.
