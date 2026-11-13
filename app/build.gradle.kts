import java.util.Properties
plugins { id("com.android.application") }
val ledgerSigning = Properties().apply {
    val f = rootProject.file(".signing/signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
android {
    namespace = "app.winku.ledger"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "app.winku.ledger"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "app.winku.ledger.LedgerInstrumentation"
    }
    if (ledgerSigning.isNotEmpty()) signingConfigs.create("ledgerRelease") {
        storeFile = rootProject.file(ledgerSigning.getProperty("storeFile"))
        storePassword = ledgerSigning.getProperty("storePassword")
        keyAlias = "winku-ledger"
        keyPassword = ledgerSigning.getProperty("storePassword")
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("ledgerRelease") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
