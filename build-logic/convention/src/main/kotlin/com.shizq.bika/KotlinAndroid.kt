package com.shizq.bika

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configure base Kotlin with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = 36

        defaultConfig.apply {
            minSdk = 23
        }

        compileOptions.apply {
            // https://developer.android.com/studio/write/java11-minimal-support-table
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

/**
 * Configure base Kotlin options
 */
private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // Treat all Kotlin warnings as errors (disabled by default)
    // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors").map {
        it.toBoolean()
    }.orElse(false)
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        jvmTarget = JvmTarget.JVM_21
        allWarningsAsErrors = warningsAsErrors
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
        freeCompilerArgs.add(
            /**
             * Remove this args after Phase 3.
             * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-consistent-copy-visibility/#deprecation-timeline
             *
             * Deprecation timeline
             * Phase 3. (Supposedly Kotlin 2.2 or Kotlin 2.3).
             * The default changes.
             * Unless ExposedCopyVisibility is used, the generated 'copy' method has the same visibility as the primary constructor.
             * The binary signature changes. The error on the declaration is no longer reported.
             * '-Xconsistent-data-class-copy-visibility' compiler flag and ConsistentCopyVisibility annotation are now unnecessary.
             */
            "-Xconsistent-data-class-copy-visibility"
        )
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")

        // 延迟、动态计算依赖情况，以根据实际模块依赖加入对应的 opt-in，消除 unresolved warning 与 needs opt-in 警告
        freeCompilerArgs.addAll(providers.provider {
            val list = mutableListOf<String>()
            
            if (hasDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core") || 
                hasDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-android")) {
                list.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                list.add("-opt-in=kotlinx.coroutines.FlowPreview")
            }
            
            if (pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization") ||
                hasDependency("org.jetbrains.kotlinx", "kotlinx-serialization-core") ||
                hasDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json")) {
                list.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
            }
            
            list
        })
    }
}

/**
 * 递归判断项目是否直接或间接（通过一级 project 依赖）引入了特定依赖，用于延迟按需配置 opt-in
 */
private fun Project.hasDependency(group: String, name: String): Boolean {
    val directMatch = configurations.any { configuration ->
        configuration.dependencies.any { dependency ->
            dependency.group == group && dependency.name == name
        }
    }
    if (directMatch) return true

    return configurations.any { configuration ->
        configuration.dependencies.any { dependency ->
            val cls = dependency::class.java
            if (cls.interfaces.any { it.name == "org.gradle.api.artifacts.ProjectDependency" } ||
                cls.name.contains("ProjectDependency")) {
                try {
                    val getDepProjectMethod = cls.getMethod("getDependencyProject")
                    val depProject = getDepProjectMethod.invoke(dependency) as? Project
                    depProject?.configurations?.any { depConfig ->
                        depConfig.dependencies.any { depDep ->
                            depDep.group == group && depDep.name == name
                        }
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }
}