// settings.gradle.kts

pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  plugins {
    id("com.android.application") version "8.11.0" apply false // Or your desired version
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false // Updated Kotlin version
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    flatDir {
      dirs("libs") // relative to root project
    }
  }
}

rootProject.name = "OakDTest"
include(":app")
