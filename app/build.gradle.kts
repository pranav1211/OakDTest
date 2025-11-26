// app/build.gradle.kts

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.oakdtest"
    compileSdk = 35 // Android 15 uses API 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.example.oakdtest"
        minSdk = 28
        targetSdk = 35 // Android 15 uses API 35
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts.add("**/libusb*.so")
            pickFirsts.add("**/libc++_shared.so")
        }
        resources {
            excludes += setOf(
                "META-INF/native-image/**",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    // Import the BoM for the Kotlin standard library
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.0"))
    // Define the dependency without the version (it will be inferred from the BoM)
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.android.material:material:1.12.0")
    implementation(project.files("libs/depthai-android.aar"))
}