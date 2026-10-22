plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.roborazzi) apply false
    // Applied here so `ktlintCheck` also covers settings.gradle.kts and this build script.
    alias(libs.plugins.ktlint)
}

// Configure Detekt for root project
detekt {
    toolVersion = libs.versions.detekt.get()
    config.from(file("detekt-mobile.yml"))
    buildUponDefaultConfig = false
    allRules = false
}

// The ktlint engine version is pinned in the catalog so both builds and the local CLI agree.
val ktlintVersion = libs.versions.ktlint.get()
allprojects {
    pluginManager.withPlugin("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(ktlintVersion)
        }
    }
}

// Configure detekt reports for all projects
allprojects {
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}

// Configure test reporting for all subprojects
subprojects {
    tasks.withType<Test>().configureEach {
        // Generate test reports for CI
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }

        // Better test output
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
        }
    }
}
