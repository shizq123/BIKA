pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://www.jitpack.io") }
    }
}
rootProject.name = "BIKA"
include(":app")
 