import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ../app.properties is the single source of truth for the release version.
val appProperties = Properties().apply {
    rootProject.file("../app.properties").inputStream().use(::load)
}
val appVersion: String = requireNotNull(appProperties.getProperty("VERSION_NAME")) {
    "VERSION_NAME is missing from app.properties"
}

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
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

group = "com.asterixorobelix.remit"
version = appVersion

application {
    mainClass.set("com.asterixorobelix.remit.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    // Always run in UTC so server-side dates never depend on the host's time zone.
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Duser.timezone=UTC")
}

ktor {
    fatJar {
        archiveFileName.set("backend-all.jar")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.body.limit)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.swagger)

    // Logging
    implementation(libs.logback.classic)

    // Payments: Firestore and Firebase Auth token verification
    implementation(libs.firebase.admin)

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.testJunit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Exposes the app.properties version to the runtime via build-info.properties.
tasks.processResources {
    val version = appVersion
    inputs.property("appVersion", version)
    filesMatching("build-info.properties") {
        expand("version" to version)
    }
}

// `./gradlew run` is a local-development entry point; deployments must set APP_ENVIRONMENT explicitly.
tasks.named<JavaExec>("run") {
    environment(
        "APP_ENVIRONMENT",
        providers.environmentVariable("APP_ENVIRONMENT").getOrElse("development"),
    )
}

// Detekt configuration
detekt {
    toolVersion = libs.versions.detekt.get()
    config.from(file("detekt.yml"))
    buildUponDefaultConfig = true
    ignoreFailures = false
}

// The ktlint engine version is pinned in the catalog so both builds and the local CLI agree.
ktlint {
    version.set(libs.versions.ktlint.get())
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        xml.required.set(true)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("user.timezone", "UTC")

    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }

    ignoreFailures = false

    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// Coverage (Kover): `./gradlew koverVerify` fails below 80% line and 70% branch coverage of the
// test run; `koverXmlReport` writes build/reports/kover/report.xml (JaCoCo format, read by the
// CI changed-lines check) and `koverHtmlReport` the browsable build/reports/kover/html/.
kover {
    reports {
        filters {
            excludes {
                // Serializable data holders and the main() entry point (tests call module() directly).
                packages("com.asterixorobelix.remit.dto")
                classes("com.asterixorobelix.remit.ApplicationKt")
            }
        }
        verify {
            rule("Line coverage") {
                minBound(80)
            }
            rule("Branch coverage") {
                minBound(70, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.koverVerify)
}

// The validation case table shared with the mobile tests (testdata/ at the repository root).
tasks.test {
    val cases = rootProject.file("../testdata/payment-validation-cases.json")
    inputs.file(cases)
    systemProperty("validationCases", cases.absolutePath)
}
