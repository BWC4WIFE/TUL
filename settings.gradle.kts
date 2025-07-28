pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
    }
    plugins {
        id("com.android.application") version "8.1.1" apply false
        id("org.jetbrains.kotlin.android") version "1.9.0" apply false
        id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false

    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "Thai Uncensored Language"
include(":app")
