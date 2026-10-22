import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties for environment-specific configuration
val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

val appProperties =
    Properties().apply {
        rootProject.file("../app.properties").inputStream().use(::load)
    }

// Helper function to get property with fallback
fun getLocalProperty(
    key: String,
    defaultValue: String,
): String = localProperties.getProperty(key) ?: System.getenv(key) ?: defaultValue

fun getAppProperty(key: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: requireNotNull(appProperties.getProperty(key)) {
        "Missing $key in app.properties"
    }

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.roborazzi)
}

kotlin {
    @Suppress("DEPRECATION")
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvmTarget.get()))
        }
    }

    // iOS ARM64 targets only (Apple Silicon Macs + all iOS devices)
    // Intel Mac simulators (iosX64) excluded - deprecated, M1+ standard since 2020
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            // Optimize for CI builds
            if (System.getenv("CI") == "true") {
                freeCompilerArgs +=
                    listOf(
                        "-Xruntime-logs=gc=info",
                        "-Xallocator=custom",
                    )
            }
        }
    }

    jvm("desktop")

    sourceSets {

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.material)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.play.review.ktx)
            implementation(libs.sqlDelight.androidDriver)
        }
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)

            implementation(libs.material.icons.extended)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.content.json)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.sqlDelight.runtime)
            implementation(libs.sqlDelight.coroutinesExtensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqlDelight.nativeDriver)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
                implementation(libs.sqlDelight.sqliteDriver)
            }
        }
        named("desktopTest") {
            dependencies {
                // Screenshot tests: offscreen Compose rendering plus Roborazzi capture/compare.
                implementation(libs.compose.ui.test)
                implementation(libs.roborazzi.compose.desktop)
            }
        }
    }
}

// Pin the generated Res package to the app package so setup.sh renames it with the
// sources; the default derives it from rootProject.name, which may not be a valid package.
compose.resources {
    packageOfResClass = "com.asterixorobelix.remit.generated.resources"
}

compose.desktop {
    application {
        mainClass = "com.asterixorobelix.remit.gallery.ComponentGalleryKt"
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.asterixorobelix.remit.data.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}

android {
    namespace = "com.asterixorobelix.remit"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        buildConfigField("String", "VERSION_NAME", "\"${getAppProperty("VERSION_NAME")}\"")
        buildConfigField("int", "VERSION_CODE", getAppProperty("VERSION_CODE"))
        buildConfigField("String", "APPLICATION_ID", "\"${getAppProperty("PACKAGE_NAME")}\"")
        buildConfigField("String", "CAPABILITIES", "\"${getAppProperty("FEATURES")}\"")
    }

    // Enable BuildConfig generation
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            // Debug environment configuration
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${getAppProperty("API_BASE_URL_DEBUG")}\"",
            )
            // Debug only: FIREBASE_EMULATOR_HOST in local.properties or the environment (10.0.2.2 from
            // the Android emulator) points Auth and Firestore at the local Firebase Emulator Suite.
            buildConfigField(
                "String",
                "FIREBASE_EMULATOR_HOST",
                "\"${getLocalProperty("FIREBASE_EMULATOR_HOST", "")}\"",
            )
            buildConfigField("boolean", "ANALYTICS_ENABLED", "false")
            buildConfigField("boolean", "CRASH_REPORTING_ENABLED", "false")
        }
        getByName("release") {
            isMinifyEnabled = false
            // Production environment configuration
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${getAppProperty("API_BASE_URL_RELEASE")}\"",
            )
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
            buildConfigField("boolean", "ANALYTICS_ENABLED", "true")
            buildConfigField("boolean", "CRASH_REPORTING_ENABLED", "true")
        }
    }

    // Staging build type (optional)
    buildTypes.create("staging") {
        initWith(buildTypes.getByName("debug"))
        buildConfigField("String", "ENVIRONMENT", "\"staging\"")
        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"\"")
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${getAppProperty("API_BASE_URL_STAGING")}\"",
        )
        buildConfigField("boolean", "ANALYTICS_ENABLED", "true")
        buildConfigField("boolean", "CRASH_REPORTING_ENABLED", "true")
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            keepDebugSymbols += "**/*.so"
        }
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.android.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    // Android Lint gate: `:composeApp:lintDebug` (debug only; release builds still run lintVital).
    // Pre-existing findings live in the committed lint-baseline.xml; anything new fails the task,
    // warnings included. Refresh the baseline with `:composeApp:updateLintBaseline` only for
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
    debugImplementation(libs.compose.ui.tooling)
    // One coverage report and floor for the app, including the shared payments module.
    kover(project(":shared"))
    // Compose-specific detekt rules (the "Compose" rule set in detekt-mobile.yml).
    detektPlugins(libs.compose.rules.detekt)
}

// `./gradlew test` (the documented gate and CI) must also run the JVM tests: NavigationGraphTest
// (Koin graph) and the screenshot tests live in desktopTest.
tasks.matching { it.name == "test" }.configureEach { dependsOn("desktopTest") }

// detekt: detekt's default rule set plus the overrides and Compose rules in detekt-mobile.yml, over
// every hand-written source set, tests included.
detekt {
    toolVersion =
        rootProject.libs.versions.detekt
            .get()
    config.from(rootProject.file("detekt-mobile.yml"))
    buildUponDefaultConfig = true
    allRules = false
    source.from(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/iosMain/kotlin",
        "src/desktopMain/kotlin",
        "src/commonTest/kotlin",
        "src/desktopTest/kotlin",
        "src/iosTest/kotlin",
    )
}

// Set JVM target for detekt to avoid compatibility issues with newer Kotlin versions
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

// ktlint covers every hand-written source set (common, platform, and test) and this script.
// SQLDelight, Compose resource accessors, and BuildConfig are generated under build/, which
// the Kotlin source sets also include, so they are filtered out here.
ktlint {
    filter {
        val generatedRoot = layout.buildDirectory.get().asFile
        exclude { element -> element.file.startsWith(generatedRoot) }
    }
}

// Coverage: `./gradlew :composeApp:koverXmlReportUnitTests` writes a JaCoCo-format XML report to
// build/reports/kover/reportUnitTests.xml from the JVM-run tests: desktopTest and the Android
// testDebugUnitTest (AGP 9 creates unit tests for the debug build type only); both run commonTest.
// The total `koverXmlReport` (build/reports/kover/report.xml) holds the same data but also compiles
// the release and staging variants. CI holds the lines a pull request changes to 80% with diff-cover;
// `koverVerifyUnitTests` (in `make check-mobile` and CI) is a total floor that stops coverage sliding,
// for example when tests are deleted. Raise it as coverage grows; never lower it to make a change pass.
kover {
    currentProject {
        createVariant("unitTests") {
            add("desktop", "debug")
        }
    }
    reports {
        variant("unitTests") {
            verify {
                rule("Line coverage floor") {
                    minBound(80)
                }
            }
        }
        filters {
            excludes {
                // Compose UI is verified by the Roborazzi screenshot tests, not line coverage.
                annotatedBy("androidx.compose.runtime.Composable")
                classes(
                    // Compiler-generated holders for composable lambdas.
                    "*.ComposableSingletons\$*",
                )
                // Generated sources: Compose resource accessors (Res, String0, ...), and the
                // SQLDelight package (generated database, queries, and row classes; its only
                // hand-written code is the SqlDriverFactory interface and platform drivers).
                packages(
                    "com.asterixorobelix.remit.generated.resources",
                    "com.asterixorobelix.remit.data.db",
                )
                // BuildConfig (plus the Activity/Fragment/databinding names the Android plugin generates).
                androidGeneratedClasses()
                // Android platform glue that plain JVM unit tests cannot run (android.jar is stubs):
                // the Application, lifecycle callbacks, and the platform actuals, i.e. `*.android.kt`
                // files and their `Android*` implementations (preferences, Log, analytics). Drop these
                // if you add Robolectric tests for them.
                inheritedFrom("android.app.Application", "android.app.Application\$ActivityLifecycleCallbacks")
                classes("*_androidKt", "*.Android*")
                // The UI kit (components, custom Modifier.Node modifiers, theme tokens) draws and
                // animates like composables do; the screenshot tests cover it. Coverage targets
                // logic: domain, data, ViewModels, navigation. Desktop platform stubs are glue
                // like the Android actuals above.
                packages(
                    "com.asterixorobelix.remit.ui.components",
                    "com.asterixorobelix.remit.ui.theme",
                )
                classes("*.platform.Console*", "*.platform.Desktop*")
                // Desktop component gallery: a developer tool whose entry point is main().
                packages("com.asterixorobelix.remit.gallery")
            }
        }
    }
}

// Screenshot tests (desktopTest, package `screenshots`). Plain `test`/`desktopTest` runs only compose
// the screens; capturing and comparing happen in the Roborazzi tasks:
//   ./gradlew :composeApp:recordRoborazziDesktop   (re)record baselines into screenshots/desktop/
//   ./gradlew :composeApp:verifyRoborazziDesktop   fail on a missing or changed screenshot
//   ./gradlew :composeApp:compareRoborazziDesktop  write diffs without failing
// Baselines are recorded on Linux CI only (font rendering differs on macOS). Diffs (*_compare.png,
// *_actual.png) go to build/outputs/roborazzi/desktop/, the HTML report to
// build/reports/roborazzi/desktop/index.html.
roborazzi {
    outputDir.set(layout.projectDirectory.dir("screenshots"))
    // One subdirectory per test task (screenshots/desktop/), so the Android unit-test variants,
    // which the plugin also configures, never share Roborazzi's directories with desktopTest.
    @OptIn(ExperimentalRoborazziApi::class)
    separateOutputDirs.set(true)
}
