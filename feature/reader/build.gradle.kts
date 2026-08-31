plugins {
    alias(libs.plugins.bika.android.feature.impl)
    alias(libs.plugins.bika.android.library.compose)
}

android {
    namespace = "com.shizq.bika.feature.reader.impl"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.download)
    implementation(libs.kotlin.logging)

    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.flowredux)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.telephoto)
}