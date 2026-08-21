plugins {
    alias(libs.plugins.bika.android.feature.impl)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.feature.reader.impl"
}

dependencies {
    implementation(projects.core.domain)
}