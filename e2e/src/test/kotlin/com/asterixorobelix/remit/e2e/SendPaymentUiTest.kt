package com.asterixorobelix.remit.e2e

import io.appium.java_client.AppiumBy
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.net.URI
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Drives the installed debug app the way a person would: UiAutomator2 finds Compose elements by
 * their test tags, which MainActivity exposes as resource ids (the names match `PaymentTestTags`).
 * Each test starts with cleared app data, so it sees onboarding and signs in as a new anonymous
 * user with an empty history.
 */
class SendPaymentUiTest {
    private lateinit var driver: AndroidDriver
    private lateinit var wait: WebDriverWait

    @BeforeTest
    fun launchApp() {
        val options = UiAutomator2Options()
            .setAppPackage(System.getProperty("app.package"))
            .setAppActivity(System.getProperty("app.activity"))
            .setNoReset(false)
            .setNewCommandTimeout(Duration.ofMinutes(2))
        driver = AndroidDriver(URI(System.getProperty("appium.url")).toURL(), options)
        wait = WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS))
    }

    @AfterTest
    fun quit() {
        if (::driver.isInitialized) driver.quit()
    }

    @Test
    fun sentPaymentAppearsInTransactionHistory() {
        skipOnboarding()
        val recipient = "e2e-${System.currentTimeMillis()}@example.com"

        tag(RECIPIENT).sendKeys(recipient)
        tag(AMOUNT).sendKeys("12.34")
        tag(SUBMIT).click()
        visible(uiSelector("textContains(\"Sent $12.34 to $recipient\")"))

        visible(uiSelector("text(\"History\")")).click()
        val row = visible(uiSelector("descriptionContains(\"$recipient\")"))
        val description = row.getAttribute("content-desc").orEmpty()
        assertTrue("$12.34" in description, description)
        assertTrue("Completed" in description, description)
    }

    @Test
    fun invalidDetailsShowFieldErrorsAndSendNothing() {
        skipOnboarding()

        tag(RECIPIENT).sendKeys("not-an-email")
        tag(AMOUNT).sendKeys("0")
        tag(SUBMIT).click()
        visible(uiSelector("text(\"Invalid recipient email\")"))
        visible(uiSelector("text(\"The amount must be more than zero\")"))

        visible(uiSelector("text(\"History\")")).click()
        visible(resourceId(HISTORY_EMPTY))
    }

    @Test
    fun currencyCanBeSwitchedToEuro() {
        skipOnboarding()
        val recipient = "e2e-eur-${System.currentTimeMillis()}@example.com"

        tag(CURRENCY).click()
        // The menu is a separate popup window, which does not expose test tags as resource ids.
        visible(uiSelector("textStartsWith(\"EUR\")")).click()
        tag(RECIPIENT).sendKeys(recipient)
        tag(AMOUNT).sendKeys("7,5")
        tag(SUBMIT).click()

        visible(uiSelector("textContains(\"Sent €7.50 to $recipient\")"))
    }

    private fun skipOnboarding() {
        visible(uiSelector("text(\"Skip\")")).click()
        visible(resourceId(SUBMIT))
    }

    private fun tag(tag: String): WebElement = visible(resourceId(tag))

    private fun visible(locator: By): WebElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator))

    private fun resourceId(id: String): By = uiSelector("resourceId(\"$id\")")

    private fun uiSelector(selector: String): By = AppiumBy.androidUIAutomator("new UiSelector().$selector")

    private companion object {
        const val TIMEOUT_SECONDS = 30L
        const val RECIPIENT = "send_recipient"
        const val AMOUNT = "send_amount"
        const val CURRENCY = "send_currency"
        const val SUBMIT = "send_submit"
        const val HISTORY_EMPTY = "history_empty"
    }
}
