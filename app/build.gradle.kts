plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.hossain.keepalive"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.hossain.keepalive"
        minSdk = 28
        /*
         * DO NOT TARGET API 35 YET - IT WILL BREAK THE APP
         * Previously, if an app held the SYSTEM_ALERT_WINDOW permission, it could launch a foreground service
         * even if the app was currently in the background (as discussed in exemptions from background start restrictions).
         * If an app targets Android 15, this exemption is now narrower. The app now needs to have the SYSTEM_ALERT_WINDOW
         * permission and also have a visible overlay window. That is, the app needs to first launch a TYPE_APPLICATION_OVERLAY
         * window and the window needs to be visible before you start a foreground service.  If your app attempts to start
         * a foreground service from the background without meeting these new requirements (and it does not have some other exemption),
         * the system throws ForegroundServiceStartNotAllowedException.  If your app declares the SYSTEM_ALERT_WINDOW
         * permission and launches foreground services from the background, it may be affected by this change.
         * If your app gets a ForegroundServiceStartNotAllowedException, check your app's order of operations and make sure
         * your app already has an active overlay window before it attempts to start a foreground service from the background.
         * You can check if your overlay window is currently visible by calling View.getWindowVisibility(),
         * or you can override View.onWindowVisibilityChanged() to get notified whenever the visibility changes.
         */
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
}

dependencies {

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.okhttp3)
    implementation(platform(libs.androidx.compose.bom))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
}
