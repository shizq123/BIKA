plugins {
    alias(libs.plugins.bika.android.library)
    alias(libs.plugins.bika.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shizq.bika.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.kotlinx.datetime)
    api(projects.core.common)
    api(projects.core.model)

    implementation(projects.core.datastore)

    implementation(libs.coil.core)
    // coil 3.5.0 编译目标为 okhttp 4.12，工程全局统一 okhttp 5.x（与 Ktor OkHttp 引擎一致），
    // 排除其传递依赖避免同一库多版本混用；coil 的 OkHttpNetworkFetcher 在 okhttp5 下运行兼容
    implementation(libs.coil.network.okhttp) {
        exclude(group = "com.squareup.okhttp3")
    }

    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.kotlinx.serialization.json)
}