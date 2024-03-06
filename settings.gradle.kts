pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://artifacts.bitmovin.com/artifactory/public-releases")
        }

    }
}

rootProject.name = "playback-sdk-android"
include(":app")
include(":playback-sdk-android")
