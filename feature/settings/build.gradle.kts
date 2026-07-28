plugins {
    alias(libs.plugins.bika.android.feature.impl)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.feature.settings.impl"
}

dependencies {
    implementation(projects.core.domain)
//    implementation(projects.feature.interests.api)
//    implementation(projects.feature.search.api)
//    implementation(projects.feature.topic.api)
//
//    testImplementation(projects.core.testing)
//
//    androidTestImplementation(libs.bundles.androidx.compose.ui.test)
//    androidTestImplementation(projects.core.testing)
}