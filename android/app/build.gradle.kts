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
