package com.asterixorobelix.remit.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The journey whose code ART should compile ahead of time: a cold start, onboarding, the Send form
 * (typing, the currency menu, a send), History, and Settings. The benchmark build is a release
 * build without Firebase, so the send and History end in their "not set up" states; the screens,
 * the form, and the snackbar are what the profile needs. Run with `make baseline-profile`.
 *
 * Elements are found by their test tags (exposed as resource ids by MainActivity) and, for the
 * onboarding and tab labels that have none, by their English text, as the Appium tests do.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = targetPackage(), includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            skipOnboarding()
            sendPayment()
            openTab("History")
            openTab("Settings")
            openTab("Send")
        }
    }

    private fun targetPackage(): String =
        requireNotNull(InstrumentationRegistry.getArguments().getString("targetAppId")) {
            "targetAppId is set in baselineprofile/build.gradle.kts; run `make baseline-profile`"
        }

    /** Onboarding shows on the first launch only; later iterations start on Send. */
    private fun MacrobenchmarkScope.skipOnboarding() {
        device.wait(Until.hasObject(By.text("Skip")), SHORT_WAIT_MS)
        device.findObject(By.text("Skip"))?.click()
        check(device.wait(Until.hasObject(By.res(RECIPIENT)), LONG_WAIT_MS)) { "Send screen did not appear" }
    }

    private fun MacrobenchmarkScope.sendPayment() {
        device.findObject(By.res(RECIPIENT)).text = "profile@example.com"
        device.findObject(By.res(AMOUNT)).text = "12.34"
        device.findObject(By.res(CURRENCY)).click()
        device.wait(Until.findObject(By.res("send_currency_EUR")), LONG_WAIT_MS)?.click()
        device.findObject(By.res(SUBMIT)).click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.openTab(label: String) {
        device.findObject(By.text(label)).click()
        device.waitForIdle()
    }

    private companion object {
        const val RECIPIENT = "send_recipient"
        const val AMOUNT = "send_amount"
        const val CURRENCY = "send_currency"
        const val SUBMIT = "send_submit"
        const val SHORT_WAIT_MS = 2_000L
        const val LONG_WAIT_MS = 5_000L
    }
}
