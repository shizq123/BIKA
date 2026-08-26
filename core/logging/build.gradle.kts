plugins {
    alias(libs.plugins.bika.android.library)
}

android {
    namespace = "com.shizq.bika.core.logging"
}

dependencies {
    api(libs.kotlin.logging)

    implementation(libs.log4j.slf4j2.impl)
    implementation(libs.log4j.core)
}