plugins {
    alias(libs.plugins.bika.android.feature.impl)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.feature.comicdetail.impl"
}

dependencies {
    implementation(projects.core.domain)
    
    implementation(libs.flowredux)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}