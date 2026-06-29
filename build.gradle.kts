plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.baselineprofile) apply false
}

// Force disable JDK image generation at the system level to avoid jlink errors
// from broken JREs provided by IDE extensions.
System.setProperty("android.jdk.generate_jdk_image", "false")
System.setProperty("android.generateJdkImage", "false")

