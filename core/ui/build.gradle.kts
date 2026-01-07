plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.core.ui"
}

dependencies {
    api(libs.androidx.metrics)
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(libs.coil.compose)
}