import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Appium UI tests for the Android app. They need a device, the Firebase emulators, the backend,
// and an Appium server, so `./gradlew test` skips them; scripts/test-appium.sh (make test-appium)
// starts Appium and runs them with -Pe2e.
plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation(libs.appium.java.client)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.testJunit5)
    testRuntimeOnly(libs.junit.platform.launcher)
    detektPlugins(libs.compose.rules.detekt)
}

val appPackage: String =
    Properties()
        .apply { rootProject.file("../app.properties").inputStream().use(::load) }
        .getProperty("PACKAGE_NAME")

tasks.test {
    useJUnitPlatform()
    val runE2e = providers.gradleProperty("e2e")
    onlyIf("needs a device and an Appium server: run make test-appium") { runE2e.isPresent }
    systemProperty("appium.url", providers.gradleProperty("appiumUrl").getOrElse("http://127.0.0.1:4723"))
    systemProperty("app.package", "$appPackage.debug")
    systemProperty("app.activity", "$appPackage.MainActivity")
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}

detekt {
    toolVersion =
        rootProject.libs.versions.detekt
            .get()
    config.from(rootProject.file("detekt-mobile.yml"))
    buildUponDefaultConfig = true
    source.from("src/test/kotlin")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}
