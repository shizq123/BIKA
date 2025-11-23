plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shizq.bika.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
}