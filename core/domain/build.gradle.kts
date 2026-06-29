plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    testImplementation(projects.core.testing)
}