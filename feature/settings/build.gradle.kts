plugins {
    alias(libs.plugins.bika.android.feature.impl)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.feature.settings.impl"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.download)

    implementation(libs.coil.compose)
    implementation(libs.flowredux)
}