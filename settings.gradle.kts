pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // JitPack per eventuali plugin non su Maven Central
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack per le librerie come com.github.tehras:charts
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Chrimametro"
include(":app")
