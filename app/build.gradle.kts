plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // KSP (Room Compiler için gerekli)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.q.dartsync"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.q.dartsync"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.firebase.database)
    // --- Room Database ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.firebase:firebase-database-ktx:20.3.0")


    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // --- Lifecycle & Coroutines ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // --- Version Catalog (libs) Bağımlılıkları ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // --- Test Birimleri ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- 🔥 MPAndroidChart (Grafik Kütüphanesi) ---
    // Not: settings.gradle.kts dosyasında JitPack ekli olduğu sürece çalışır.
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
}