plugins {
    alias(libs.plugins.bika.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.shizq.bika.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    testImplementation(projects.core.testing)
}