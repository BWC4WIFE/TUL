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
    }
    plugins {
        id("com.android.application") version "8.1.1" apply false
        id("org.jetbrains.kotlin.android") version "1.9.0" apply false
        id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
        id("org.gradle.toolchains.foojay-resolver-convention") version("0.9.0")

    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Thai Uncensored Language"
include(":app")
