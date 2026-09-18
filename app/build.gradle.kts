plugins { id("com.android.application") }

android {
    namespace = "com.usbprint.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.usbprint.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    buildTypes { release { isMinifyEnabled = false } }
}