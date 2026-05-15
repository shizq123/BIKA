plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shizq.bika.core.data"
}

dependencies {
    implementation(libs.kotlinx.serialization.core)
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.network)
    implementation(libs.okhttp)
    implementation("com.squareup.okio:okio:3.9.0")

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}