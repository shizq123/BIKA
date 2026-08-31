plugins {
    alias(libs.plugins.bika.android.library)
}

android {
    namespace = "com.shizq.bika.core.logging"
}

dependencies {
    api(libs.kotlin.logging)

    implementation(libs.log4j.core)
    implementation(libs.log4j.slf4j.impl)
    implementation("com.github.tony19:logback-android:3.0.0")
}