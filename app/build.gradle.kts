plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stackunderflow.airgap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stackunderflow.airgap"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Chinmay's files stay exactly where they are - we just point at them.
    // Nobody has to move anything, so no merge conflicts.
    sourceSets["main"].apply {
        java.srcDirs("../airgap-src/java")
        res.srcDirs("../airgap-src/res")
        assets.srcDirs("../airgap-src/assets")
        manifest.srcFile("../airgap-src/AndroidManifest.xml")
    }

    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.google.mediapipe:tasks-genai:0.10.29")   // on-device Gemma
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
}
