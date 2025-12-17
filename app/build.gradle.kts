plugins {
    alias(libs.plugins.android.application)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs")
}

android {
    namespace = "com.example.volunhub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.volunhub"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    // Add the dependency for the Firebase SDK for Google Analytics
    implementation("com.google.firebase:firebase-analytics")

    // For Firebase Authentication (Login/Signup)
    implementation("com.google.firebase:firebase-auth")

    // For Firestore Database (To store jobs, users, etc.)
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.cloudinary:cloudinary-android:3.1.2")
    // Java versions of Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("androidx.core:core-ktx:1.12.0")

    // Also update these for better compatibility
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}