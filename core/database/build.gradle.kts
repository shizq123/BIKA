plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.android.room)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.database"
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.datetime)

    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.0")

    androidTestImplementation(libs.kotlinx.coroutines.test)
}