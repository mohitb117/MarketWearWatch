plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.marketwear.mobile"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.marketwear.indices"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"
    }
}
