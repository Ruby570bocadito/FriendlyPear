plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun localProperty(name: String, default: String) = (project.findProperty(name) as String?) ?: default

android {
    namespace = "app.friendlypear"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.friendlypear"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "BACKEND_URL", "\"${localProperty("friendlypear.backendUrl", "http://10.0.2.2:8787")}\"")
        // Shared secret with the backend (DEVICE_TOKEN). Lives in ~/.gradle/gradle.properties, never in this repo;
        // empty means the second opinion is off and the rules decide alone.
        buildConfigField("String", "DEVICE_TOKEN", "\"${localProperty("friendlypear.deviceToken", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("app.friendlypear:core:0.1.0")
}
