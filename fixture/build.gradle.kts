plugins { id("com.android.application") }
android {
    namespace = "fixture.winku.notifications"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig { applicationId = "com.tencent.mm"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "test-only" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
