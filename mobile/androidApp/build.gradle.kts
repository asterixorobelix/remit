import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

val appProperties = Properties().apply {
    rootProject.file("../app.properties").inputStream().use(::load)
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
}

// Strings that only appear in the placeholder google-services.json of older template versions.
val firebasePlaceholderMarkers = listOf("DEFAULT TEMPLATE", "your-project-id-here")

// Firebase is opt-in: `firebase` must be listed in FEATURES (app.properties) AND a real
// google-services.json must exist next to this file (it is gitignored; CI writes it from the
// GOOGLE_SERVICES_JSON secret). Without it, builds simply skip Firebase.
val firebaseCapability = appProperties.getProperty("FEATURES").orEmpty()
    .split(',')
    .any { it.trim().equals("firebase", ignoreCase = true) }
val googleServicesFile = file("google-services.json")
val hasRealGoogleServices = googleServicesFile.exists() &&
    googleServicesFile.readText().let { json ->
        firebasePlaceholderMarkers.none { marker -> json.contains(marker) }
    }
val firebaseEnabled = firebaseCapability && hasRealGoogleServices

// Without google-services.json, debug builds can use the local Firebase Emulator Suite instead
// (composeApp turns the same key into BuildConfig.FIREBASE_EMULATOR_HOST).
val firebaseEmulatorHost = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}.getProperty("FIREBASE_EMULATOR_HOST") ?: System.getenv("FIREBASE_EMULATOR_HOST").orEmpty()
if (firebaseCapability && !hasRealGoogleServices) {
    if (firebaseEmulatorHost.isBlank()) {
        logger.warn(
            """
            |WARNING: Firebase is not set up, so Send and History will fail in the app.
            |  Local development: run `make emulators` and `make backend-emulated`, then
            |    echo 'FIREBASE_EMULATOR_HOST=10.0.2.2' >> mobile/local.properties
            |  and rebuild (the host is compiled into debug builds).
            |  Real project: add androidApp/google-services.json (see "Run it" in README.md).
            """.trimMargin(),
        )
    } else {
        logger.info(
            "No androidApp/google-services.json: debug builds use the Firebase emulators at " +
                "$firebaseEmulatorHost; release builds have no Firebase.",
        )
    }
}
if (firebaseEnabled) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvmTarget.get()))
    }
}

extensions.configure<ApplicationExtension> {
    namespace = "com.asterixorobelix.remit.app"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = appProperties.getProperty("PACKAGE_NAME")
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = appProperties.getProperty("VERSION_CODE").toInt()
        versionName = appProperties.getProperty("VERSION_NAME")
    }

    // Release signing configuration - optional until keystore is provided
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val hasKeystore = keystorePropertiesFile.exists() || System.getenv("KEYSTORE_FILE") != null

    if (hasKeystore) {
        signingConfigs {
            create("release") {
                if (keystorePropertiesFile.exists()) {
                    val keystoreProperties = Properties()
                    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                    storeFile = file(keystoreProperties["storeFile"].toString())
                    storePassword = keystoreProperties["storePassword"].toString()
                    keyAlias = keystoreProperties["keyAlias"].toString()
                    keyPassword = keystoreProperties["keyPassword"].toString()
                } else {
                    // CI/CD environment variables
                    storeFile = file(System.getenv("KEYSTORE_FILE")!!)
                    storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                    keyAlias = System.getenv("KEY_ALIAS") ?: ""
                    keyPassword = System.getenv("KEY_PASSWORD") ?: ""
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += "**/*.so"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Staging build type (optional)
    buildTypes.create("staging") {
        initWith(buildTypes.getByName("debug"))
        applicationIdSuffix = ".staging"
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.android.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    // Android Lint gate: `:androidApp:lintDebug` (debug only; release builds still run lintVital).
    // Pre-existing findings live in the committed lint-baseline.xml; anything new fails the task,
    // warnings included. Refresh the baseline with `:androidApp:updateLintBaseline` only for
    // findings you have decided to accept.
    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        disable +=
            setOf(
                // Version-freshness checks depend on the day the build runs, not on the change;
                // dependency updates are handled separately (version catalog + `check deps`).
                "AndroidGradlePluginVersion",
                "GradleDependency",
                "NewerVersionAvailable",
                "OldTargetApi",
            )
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.koin.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    debugImplementation(libs.compose.ui.tooling)

    // Compose-specific detekt rules (the "Compose" rule set in detekt-mobile.yml).
    detektPlugins(libs.compose.rules.detekt)

    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core.ktx)

    // Firebase SDKs are only linked when the Google Services plugin is applied (see above).
    if (firebaseEnabled) {
        implementation(project.dependencies.platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
        implementation(libs.firebase.analytics)
    }
}

// Same detekt rules as :composeApp (detekt-mobile.yml on top of detekt's defaults).
detekt {
    toolVersion = libs.versions.detekt.get()
    config.from(rootProject.file("detekt-mobile.yml"))
    buildUponDefaultConfig = true
    source.from("src/main/kotlin", "src/androidTest/kotlin")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}
