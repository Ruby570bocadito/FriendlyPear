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

    signingConfigs {
        // Our own key, kept outside this repo: ~/.gradle/gradle.properties says where it is and its passwords. Without
        // them the release build is simply unsigned.
        val keystore = localProperty("friendlypear.releaseKeystore", "")
        if (keystore.isNotEmpty()) {
            create("release") {
                storeFile = file(keystore)
                storePassword = localProperty("friendlypear.releaseStorePassword", "")
                keyAlias = localProperty("friendlypear.releaseKeyAlias", "friendlypear")
                keyPassword = localProperty("friendlypear.releaseKeyPassword", "")
            }
        }
    }

    buildTypes {
        // The shared test token (DEVICE_TOKEN) only in debug builds, from ~/.gradle/gradle.properties, never in this
        // repo; a release build never carries it: only phones paired in person get in.
        // Screens a scammer would want to read stay out of captures and screen sharing (Guard.kt): always in release;
        // debug builds stay capturable for the tests and the video, unless -Pfriendlypear.secureScreens=true.
        debug {
            buildConfigField("String", "DEVICE_TOKEN", "\"${localProperty("friendlypear.deviceToken", "")}\"")
            buildConfigField("boolean", "SECURE_SCREENS", localProperty("friendlypear.secureScreens", "false"))
        }
        // The one that is shown and handed out: signed with our key, its code and resources shrunk by R8.
        release {
            buildConfigField("String", "DEVICE_TOKEN", "\"\"")
            buildConfigField("boolean", "SECURE_SCREENS", "true")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            // Rive's native library for phones (ARM, 64 and 32 bits: Android Go phones run 32) and today's emulators,
            // not for 32-bit x86, which no Android 12 phone uses: about 5 MB less to download.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            }
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
    // Rive's runtime (MIT License): the vector animations in res/raw/rive_*.riv, made by assets/rive. 10.4.0 is the
    // last one that builds against compileSdk 35.
    implementation("app.rive:rive-android:10.4.0")
    // The lifecycle Rive's views need to give their memory back (RiveArt.kt): the version Rive already brings.
    implementation("androidx.lifecycle:lifecycle-runtime:2.9.1")
}
