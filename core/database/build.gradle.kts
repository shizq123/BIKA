plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.android.room)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.database"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.datetime)

    androidTestImplementation(libs.kotlinx.coroutines.test)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}