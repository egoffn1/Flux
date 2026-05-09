plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.fluxmusic.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fluxmusic.player"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")

            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeVersion = "1.5.4"
    val roomVersion = "2.6.1"
    val media3Version = "1.2.0"
    val hiltVersion = "2.48.1"

    coreKtx("androidx.core:core-ktx:1.12.0")
    lifecycle("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    lifecycle("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    activity("androidx.activity:activity-compose:1.8.2")

    composeBom("androidx.compose:compose-bom:$composeVersion")
    compose("androidx.compose.ui:ui")
    compose("androidx.compose.ui:ui-graphics")
    compose("androidx.compose.ui:ui-tooling-preview")
    compose("androidx.compose.material3:material3")
    compose("androidx.compose.material:material-icons-extended")
    compose("androidx.compose.animation:animation")

    navigation("androidx.navigation:navigation-compose:2.7.6")

    media3("androidx.media3:media3-exoplayer:$media3Version")
    media3("androidx.media3:media3-session:$media3Version")
    media3("androidx.media3:media3-ui:$media3Version")

    room("androidx.room:room-runtime:$roomVersion")
    room("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    datastore("androidx.datastore:datastore-preferences:1.0.0")

    hilt("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    compose("androidx.hilt:hilt-navigation-compose:1.1.0")

    coil("io.coil-kt:coil-compose:2.5.0")

    palette("androidx.palette:palette-ktx:1.0.0")

    coroutines("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testing("androidx.test.ext:junit:1.1.5")
    testing("androidx.test.espresso:espresso-core:3.5.1")
    testing("androidx.compose.ui:ui-test-junit4")
    debug("androidx.compose.ui:ui-tooling")
    debug("androidx.compose.ui:ui-test-manifest")
}