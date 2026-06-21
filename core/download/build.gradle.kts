plugins {
    alias(libs.plugins.bika.android.library)
}

android {
    namespace = "com.shizq.bika.core.download"
}

dependencies {
    api(libs.kotlinx.datetime)
}