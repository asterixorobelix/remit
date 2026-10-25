import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The payments feature with no UI: models, validation, use cases, the API client, and Firebase
// (expect/actual). composeApp builds the Compose UI on top of it; a SwiftUI or desktop app could
// use it without Compose.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.androidLint)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.asterixorobelix.remit.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.android.jvmTarget.get()))
        }
        // commonTest also runs on the Android JVM (the Firebase actuals stay out: android.jar is stubs).
        withHostTest {}
        lint {
            abortOnError = true
            warningsAsErrors = true
            checkReleaseBuilds = true
            disable +=
                setOf(
                    "AndroidGradlePluginVersion",
                    "GradleDependency",
                    "NewerVersionAvailable",
                    "OldTargetApi",
                )
        }
    }
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    compilerOptions {
        optIn.addAll("kotlin.time.ExperimentalTime", "kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
        }
        // Cucumber BDD scenarios (src/desktopTest/resources/features) run on the JUnit Platform.
        named("desktopTest") {
            dependencies {
                implementation(project.dependencies.platform(libs.junit.bom))
                implementation(libs.cucumber.java)
                implementation(libs.cucumber.junit.platform.engine)
                implementation(libs.junit.platform.suite)
                implementation(libs.ktor.client.mock)
            }
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.kotlinx.coroutines.play.services)
        }
    }
}

// detekt-mobile.yml configures the Compose rule set, so detekt needs the plugin to validate the config
// even though this module has no Compose code.
dependencies {
    detektPlugins(libs.compose.rules.detekt)
}

// `./gradlew test` also runs the JVM tests in desktopTest, as in composeApp.
tasks.matching { it.name == "test" }.configureEach { dependsOn("desktopTest") }

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
    )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
}

ktlint {
    filter {
        val generatedRoot = layout.buildDirectory.get().asFile
        exclude { element -> element.file.startsWith(generatedRoot) }
    }
}

// Same "unitTests" variant as composeApp (desktop JVM plus Android debug unit tests), so composeApp
// merges this module into its report. The floor is higher here: this module is the business logic.
// The Android actuals wrap the Firebase SDK, which plain JVM tests cannot run (android.jar is stubs).
kover {
    currentProject {
        createVariant("unitTests") {
            add("desktop", "android")
        }
    }
    reports {
        filters {
            excludes {
                classes("*_androidKt", "*.Firebase*", "*.Firestore*")
            }
        }
        variant("unitTests") {
            verify {
                rule("Line coverage floor") {
                    minBound(90)
                }
            }
        }
    }
}

// The validation case table shared with the backend tests (testdata/ at the repository root).
tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
    // Cucumber's readable scenario report, next to Gradle's test report.
    systemProperty("cucumber.plugin", "pretty, html:build/reports/cucumber/index.html")
    val cases = rootProject.file("../testdata/payment-validation-cases.json")
    inputs.file(cases)
    systemProperty("validationCases", cases.absolutePath)
}
