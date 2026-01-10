plugins {
    alias(libs.plugins.bika.android.application)
    alias(libs.plugins.bika.android.application.compose)
    alias(libs.plugins.bika.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("kotlin-kapt")
}

android {
    namespace = "com.shizq.bika"

    defaultConfig {
        applicationId = "com.shizq.bika"
        versionCode = 13
        versionName = "1.10.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("keyStore") {
            storeFile = file("appkey.jks")
            storePassword = "123456"
            keyAlias = "shizq"
            keyPassword = "123456"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("keyStore")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "retrofit2.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }

    applicationVariants.all {
        outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "BIKA_v${versionName}.apk"
            }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.savedstate.compose)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.window.core)
    implementation(libs.coil.kt)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.byrecyclerview)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.adapter.rxjava3)
    implementation(libs.retrofit.converter.kotlinx.serialization)

    implementation(libs.glide)

    ksp(libs.glide.compiler)
    implementation(libs.glide.okhttp3)

    implementation(libs.rxandroid)

    implementation(libs.pictureselector)
    implementation(libs.ucrop)

    implementation(libs.photoview)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.commons.codec)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.retrofit.converter.kotlinx.serialization)

    implementation(libs.coil.compose)

    implementation(libs.reorderable)

    implementation(libs.flowredux)

    implementation(kotlin("test"))
    implementation(kotlin("test-junit"))

    implementation("me.saket.telephoto:zoomable:0.18.0")

    kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")
}
