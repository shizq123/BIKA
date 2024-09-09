plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
//    id("kotlinx-serialization")
}

android {
    namespace = "com.shizq.bika"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shizq.bika"
        minSdk = 24
        targetSdk = 34
        versionCode = 8
        versionName = "1.0.7"

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
        debug {
            signingConfig = signingConfigs.getByName("keyStore")
        }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.5")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.github.youlookwhat:ByRecyclerView:1.3.6")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.14.2")

    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

    implementation("io.github.lucksiege:pictureselector:v3.10.7")
    implementation("io.github.lucksiege:ucrop:v3.10.7")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

}