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
        // No need for JitPack here unless you have a Gradle plugin from JitPack
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // <<< THIS IS WHERE JITPACK BELONGS FOR PROJECT DEPENDENCIES
        maven("https://jitpack.io")
    }
}

rootProject.name = "Blue Phoenix"
include(":app")