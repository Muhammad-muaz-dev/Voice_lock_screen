plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.futurecode.voicelockscreen"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.futurecode.voicelockscreen"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation(libs.firebase.crashlytics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")


    implementation("androidx.work:work-runtime-ktx:2.11.2")


    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.4")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    //google billing
    implementation("com.android.billingclient:billing-ktx:8.3.0")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Material components (needed for ShapeableImageView/Ripple)
    implementation("com.google.android.material:material:1.11.0")

    // Lottie dependency
    implementation("com.airbnb.android:lottie:6.4.1")
    // On-device note translation (ML Kit)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)

    //glide
    implementation ("com.github.bumptech.glide:glide:5.0.7")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.7")

    // firebase analytics
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    // Remote switches for ad placements
    implementation("com.google.firebase:firebase-config")

    // Google Mobile Ads – Next-Gen SDK
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.7.0")



}
