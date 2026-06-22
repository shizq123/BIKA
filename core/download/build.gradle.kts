plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.download"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.datastore)

    api(libs.kotlinx.datetime)

    implementation(libs.okhttp)
    implementation(libs.okio)
}