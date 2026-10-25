# R8 rules for the release build.
#
# Kotlin, kotlinx.serialization, Ktor, OkHttp, coroutines, Koin, Compose, Navigation, SQLDelight,
# and Firebase ship their own consumer rules, so nothing here keeps a whole library or package:
# a blanket -keep switches off shrinking and optimization for everything it matches. Add a rule
# only for code that is reached by reflection, and say which code.

# Readable stack traces: Crashlytics deobfuscates with the uploaded mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Release builds drop verbose, debug, and info logging, so a stray Log.d never reaches production
# (AndroidLogger also stops at WARN there; see AppConfig.minLogLevel). Warnings and errors stay.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Firebase finds its components by reflection: ComponentDiscovery reads the registrar class names
# from the manifest and calls their no-argument constructors. Some Firebase SDKs (Crashlytics,
# Installations) keep the registrar classes but not those constructors, which R8 full mode then
# removes, so Crashlytics would silently never start in release builds.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
