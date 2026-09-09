plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.core.ui"
}

dependencies {
    api(projects.core.common)
    api(projects.core.designsystem)
    api(libs.androidx.metrics)
    api(projects.core.model)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    androidTestImplementation(libs.bundles.androidx.compose.ui.test)
    androidTestImplementation(projects.core.testing)
}