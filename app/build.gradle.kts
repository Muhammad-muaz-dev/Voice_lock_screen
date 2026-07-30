plugins {
    alias(libs.plugins.android.application)
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //facebook ads
    implementation("com.facebook.android:audience-network-sdk:6.21.0")

    //google ads
    implementation("com.google.android.gms:play-services-ads:25.2.0")
    implementation("com.google.ads.mediation:facebook:6.16.0.0")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")

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

    //glide
    implementation ("com.github.bumptech.glide:glide:5.0.7")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.7")


    // firebase analytics
//    implementation("com.google.firebase:firebase-analytics")
//    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))



}
