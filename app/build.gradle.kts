import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.prescriptaai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.prescriptaai"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // NEW ROBUST LINE - PREVENTS THE CRASH
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        // I am using XML Views, not Compose
        compose = false
        viewBinding = true
        buildConfig=true
    }
}

dependencies {
    // Default dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // For easier asynchronous programming (Coroutines)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // For easier ViewModel and LiveData implementation (Lifecycle components)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // --- FIREBASE DEPENDENCIES ---

    // 1. IMPORT THE FIREBASE BOM (Bill of Materials) FIRST
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))

    // 2. NOW ADD THE FIREBASE PRODUCTS YOU WANT TO USE (without versions)
    // The BoM will manage the versions for you.
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Inside the dependencies block
    implementation("com.google.ai.client.generativeai:generativeai:0.3.0")
    implementation(libs.androidx.activity)

    // --- CAMERA X DEPENDENCIES ---
    val cameraxVersion = "1.3.3" // Or the latest version
    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")

    implementation("com.google.code.gson:gson:2.10.1")


}