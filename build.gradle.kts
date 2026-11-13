plugins {
    id("com.android.application") version "9.4.1" apply false
    // Align AGP's build-time Kotlin toolchain with the installed SDK toolchain.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
