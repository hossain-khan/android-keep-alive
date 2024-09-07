package dev.hossain.keepalive.util

object AppConfig {
    const val MINIMUM_APP_CHECK_INTERVAL_MIN = 5
    const val DEFAULT_APP_CHECK_INTERVAL_MIN = 30

    // List of supported devices - hardcoded for now.
    const val PHONE_OG_PIXEL = "Pixel XL"
    const val PHONE_GS23 = "SM-S911W"

    const val OG_PIXEL_URL = "https://hc-ping.com/357a4e95-a7b3-4cd0-9506-4168fd9f1794"
    const val GS23_URL = "https://hc-ping.com/c1194e83-99d2-447b-8043-520320fc4c39"

    val phoneToUrlMap =
        hashMapOf(
            PHONE_OG_PIXEL to OG_PIXEL_URL,
            PHONE_GS23 to GS23_URL,
        )

    // https://play.google.com/store/apps/details?id=com.nutomic.syncthingandroid
    const val SYNC_APP_PACKAGE_NAME = "com.nutomic.syncthingandroid"
    const val SYNC_APP_LAUNCH_ACTIVITY =
        "com.nutomic.syncthingandroid.activities.FirstStartActivity"

    // https://play.google.com/store/apps/details?id=com.google.android.apps.photos
    const val PHOTOS_APP_PACKAGE_NAME =
        "com.google.android.apps.photos"
    const val PHOTOS_APP_LAUNCH_ACTIVITY =
        "com.google.android.apps.photos.home.HomeActivity"
}
