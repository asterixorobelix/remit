import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Generates androidApp's Baseline Profile: the classes and methods ART compiles ahead of time at
// install, so startup and the first frames of each screen are not interpreted. Run it on a
// connected emulator or device (API 33+, or rooted API 28+) with `make baseline-profile`; the
// profile is written to androidApp/src/release/generated/baselineProfiles/ and committed.
plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineProfile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvmTarget.get()))
    }
}

android {
    namespace = "com.asterixorobelix.remit.baselineprofile"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        // Baseline Profile collection needs API 28+; the app itself still runs from API 24.
        minSdk = 28
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.android.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    targetProjectPath = ":androidApp"
}

// Use the emulator or device that is already connected instead of a Gradle Managed Device.
baselineProfile {
    useConnectedDevices = true
}

// Tells the generator which app to drive (the benchmark build's application id).
androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
        )
    }
}

dependencies {
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)

    // detekt-mobile.yml configures the Compose rule set, so detekt needs the plugin to validate it.
    detektPlugins(libs.compose.rules.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.from(rootProject.file("detekt-mobile.yml"))
    buildUponDefaultConfig = true
    source.from("src/main/kotlin")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}
