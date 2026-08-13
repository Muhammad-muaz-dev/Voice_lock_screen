# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


#=============================================================================
# Global Optimization Attributes & Crash Log Debug Safeties
#=============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Global attributes required by serialization frameworks (GSON, Retrofit, Coroutines)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, RuntimeVisibleStringAnnotations, AnnotationDefault

#=============================================================================
# Android System, Architecture Baseline & ViewBinding Rules
#=============================================================================
# Keep ViewBinding implementations safe from reflection breakages
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# Keep Custom Views & Lock Views safe from obfuscation
-keep class com.futurecode.voicelockscreen.ui.custom.** { *; }

# Keep Navigation Component safe
-keep class * extends androidx.navigation.NavArgs

# Keep Enum values and Serializable fields from being stripped
-keepclassmembers enum * { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#=============================================================================
# Dependency: AndroidX WorkManager (2.11.2)
#=============================================================================
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

#=============================================================================
# Dependency: AndroidX Biometric (1.2.0)
#=============================================================================
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

#=============================================================================
# Dependency: Hilt & Dagger Dependency Injection
#=============================================================================
-keep class * extends dagger.hilt.internal.UnsafeCasts
-keep class dagger.hilt.** { *; }
-keep class androidx.hilt.** { *; }
-dontwarn androidx.hilt.**
-dontwarn dagger.hilt.**

#=============================================================================
# Dependency: Gson & Data Serialization Models
#=============================================================================
-dontwarn com.google.gson.**
-keep class com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.TypeAdapter

# Safeguard any class member utilizing Gson SerializedName annotation fields
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep project data models safe
-keep class com.futurecode.voicelockscreen.model.** { *; }
-keep class com.futurecode.voicelockscreen.notification.** { *; }

#=============================================================================
# Dependency: Retrofit 2 & OkHttp 3
#=============================================================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp platform checks warnings suppression
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

#=============================================================================
# Dependency: Google Play Mobile Ads SDK (AdMob 25.2.0) & Facebook Mediation
#=============================================================================
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.google.ads.mediation.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**

#=============================================================================
# Dependency: Google Firebase Analytics (BoM 34.13.0)
#=============================================================================
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.firebase.**

#=============================================================================
# Dependency: Google Play Billing Client (Billing-KTX 8.3.0)
#=============================================================================
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

#=============================================================================
# Dependency: Glide (Image Loading framework 5.0.7)
#=============================================================================
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-dontwarn com.bumptech.glide.GeneratedAppGlideModuleImpl
-dontwarn com.github.bumptech.glide.**

#=============================================================================
# Dependency: Lottie Animations (6.4.1)
#=============================================================================
-keep class com.airbnb.lottie.** { *; }

#=============================================================================
# Dependency: Intuit Responsive Dimens (SDP 1.1.1 / SSP 1.1.1)
#=============================================================================
-keep class com.intuit.sdp.** { *; }
-keep class com.intuit.ssp.** { *; }

#=============================================================================
# Dependency: Kotlin Coroutines Safeties
#=============================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}