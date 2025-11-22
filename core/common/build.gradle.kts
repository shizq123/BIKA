plugins {
    alias(libs.plugins.bika.android.library)
}

android {
    namespace = "com.shizq.bika.core.model"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
}