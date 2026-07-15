plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.voicemate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.voicemate"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lottie)

    // ML Kit dependencies
    implementation(libs.mlkit.text.recognition)
    implementation("com.google.mlkit:object-detection:17.0.2")

    // CameraX dependencies
    val cameraXVersion = "1.3.1"

    implementation(
        "androidx.camera:camera-core:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-camera2:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-lifecycle:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-view:$cameraXVersion"
    )
    implementation(
        "androidx.camera:camera-extensions:$cameraXVersion"
    )

    // Firebase and Google Authentication
    implementation(
        platform("com.google.firebase:firebase-bom:32.7.0")
    )
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation(
        "com.google.android.gms:play-services-auth:20.7.0"
    )
    implementation("com.google.firebase:firebase-firestore-ktx")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
