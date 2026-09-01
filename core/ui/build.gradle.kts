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
    implementation(libs.coil.network.okhttp)
    implementation(projects.core.common)
    implementation(libs.kotlin.logging)

    androidTestImplementation(libs.bundles.androidx.compose.ui.test)
    androidTestImplementation(projects.core.testing)
}