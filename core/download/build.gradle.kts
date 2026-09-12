plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.download"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.network)

    api(libs.kotlinx.datetime)

    implementation(libs.okhttp)
    implementation(libs.okio)

    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)
    ksp(libs.hilt.ext.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}