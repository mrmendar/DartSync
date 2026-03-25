pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MPAndroidChart için gerekli olan ana depo:
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "DartSync"
include(":app")