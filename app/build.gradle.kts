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
        minSdk = 24 //最低支持Android7.0
        targetSdk = 34
        versionCode = 6
        versionName = "1.0.5"

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

        getByName("debug") {
            signingConfig = signingConfigs.getByName("keyStore")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("keyStore")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
        val variant = this
//        val versionCodes =
//            mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

        variant.outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
//                val abi = if (output.getFilter("ABI") != null)
//                    output.getFilter("ABI")
//                else
//                    "all"

                output.outputFileName = "BIKA_v${variant.versionName}.apk"
//                if(versionCodes.containsKey(abi))
//                {
//                    output.versionCodeOverride = (1000000 * versionCodes[abi]!!).plus(variant.versionCode)
//                }
//                else
//                {
//                    return@forEach
//                }
            }
    }


}

dependencies {
    implementation("androidx.test:core-ktx:1.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.preference:preference-ktx:1.2.1")//设置页
    implementation("androidx.core:core-splashscreen:1.0.1")//启动页
//    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.4.1" //序列化

    implementation("androidx.room:room-runtime:2.6.1") //数据库
    kapt("androidx.room:room-compiler:2.6.1") //数据库

    implementation("com.github.youlookwhat:ByRecyclerView:1.3.6")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")

    implementation("com.github.bumptech.glide:glide:4.14.2")//  Glide
    kapt("com.github.bumptech.glide:compiler:4.14.2") //  Glide
    implementation("com.github.bumptech.glide:okhttp3-integration:4.14.2")//  Glide

    implementation("io.reactivex.rxjava3:rxjava:3.1.5")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.0")

    implementation("io.github.lucksiege:pictureselector:v3.10.7")//图片选择器
    implementation("io.github.lucksiege:ucrop:v3.10.7")//修剪

    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

}