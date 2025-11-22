plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
}