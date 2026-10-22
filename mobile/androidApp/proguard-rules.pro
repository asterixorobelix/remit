# ProGuard rules for Kotlin Multiplatform Mobile App
# These rules are applied when minifyEnabled = true in release builds

# ============================================================================
# KOTLIN & SERIALIZATION
# ============================================================================

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable classes in the app package (setup.sh renames the package).
-keep,includedescriptorclasses class com.example.myproject.**$$serializer { *; }
-keepclassmembers class com.example.myproject.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.myproject.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep model classes used for serialization
-keep class com.example.myproject.models.** { *; }
-keep class com.example.myproject.network.** { *; }
-keep class com.example.myproject.data.** { *; }

# ============================================================================
# KTOR CLIENT
# ============================================================================

-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Ktor debugging classes (not available on Android)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ============================================================================
# OKHTTP (used by Ktor on Android)
# ============================================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ============================================================================
# KOIN DEPENDENCY INJECTION
# ============================================================================

-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# ============================================================================
# FIREBASE CRASHLYTICS
# ============================================================================

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep exception classes
-keep public class * extends java.lang.Exception

-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# The shared module talks to Firebase through reflection (composeApp has no compile-time
# Firebase dependency), so R8 must keep the reflectively-invoked entry points.
-keep class com.google.firebase.FirebaseApp {
    public static com.google.firebase.FirebaseApp getInstance();
    public android.content.Context getApplicationContext();
}
-keep class com.google.firebase.analytics.FirebaseAnalytics {
    public static com.google.firebase.analytics.FirebaseAnalytics getInstance(android.content.Context);
    public void logEvent(java.lang.String, android.os.Bundle);
    public void setUserProperty(java.lang.String, java.lang.String);
    public void setUserId(java.lang.String);
    public void setAnalyticsCollectionEnabled(boolean);
    public void resetAnalyticsData();
}
-dontwarn com.google.firebase.analytics.**

# ============================================================================
# COMPOSE
# ============================================================================

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# ANDROID COMMON
# ============================================================================

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release (optional - uncomment if desired)
# -assumenosideeffects class android.util.Log {
#     public static int v(...);
#     public static int d(...);
#     public static int i(...);
# }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# GOOGLE PLAY SERVICES
# ============================================================================

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ============================================================================
# ADDITIONAL RULES
# ============================================================================

# Add project-specific rules below as needed
