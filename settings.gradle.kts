pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
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
include(":core:download")
include(":core:model")
include(":core:network")
include(":core:testing")
include(":core:ui")

include(":sync:work")

includeBuild("build-logic")
