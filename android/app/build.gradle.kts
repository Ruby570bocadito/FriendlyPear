plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun localProperty(name: String, default: String) = (project.findProperty(name) as String?) ?: default

android {
    // The language is chosen inside the app (Settings), so every language must ship in the APK or bundle.
    bundle {
        language {
            enableSplit = false
        }
    }

    namespace = "app.friendlypear"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.friendlypear"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "BACKEND_URL", "\"${localProperty("friendlypear.backendUrl", "http://10.0.2.2:8787")}\"")
    }

    buildTypes {
        // The shared test token (DEVICE_TOKEN) only in debug builds, from ~/.gradle/gradle.properties, never in this
        // repo; a release build never carries it: only phones paired in person get in.
        debug {
            buildConfigField("String", "DEVICE_TOKEN", "\"${localProperty("friendlypear.deviceToken", "")}\"")
        }
        release {
            buildConfigField("String", "DEVICE_TOKEN", "\"\"")
        }
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
    // Reads QR codes in a picture on the phone, offline (Apache License 2.0).
    implementation("com.google.zxing:core:3.5.4")
}
