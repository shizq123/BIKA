plugins {
    alias(libs.plugins.bika.android.library)
}

android {
    namespace = "com.shizq.bika.core.logging"
    packagingOptions {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    api(libs.kotlin.logging)

    implementation(libs.log4j.slf4j2.impl)
    implementation(libs.log4j.core)
}