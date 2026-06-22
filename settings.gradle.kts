pluginManagement {
    val isCI = System.getenv("CI") == "true"
    includeBuild("build-logic")
    repositories {
        if (!isCI) {
            maven { setUrl("https://maven.aliyun.com/repository/google") }
            maven { setUrl("https://maven.aliyun.com/repository/public") }
            maven { setUrl("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/google/") }
            maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    val isCI = System.getenv("CI") == "true"
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        if (!isCI) {
            maven { setUrl("https://maven.aliyun.com/repository/google") }
            maven { setUrl("https://maven.aliyun.com/repository/public") }
            maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/google/") }
            maven { setUrl("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
    }
}

rootProject.name = "BIKA"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":benchmarks")
include(":core:common")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:testing")
include(":core:ui")

include(":sync:work")