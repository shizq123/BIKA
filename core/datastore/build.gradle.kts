plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.datastore"
}

dependencies {
    api(libs.androidx.dataStore)
    api(projects.core.model)

    implementation(projects.core.common)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.coroutines.test)
}