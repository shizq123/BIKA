plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
}

android {
    namespace = "com.shizq.bika.core.common"
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
