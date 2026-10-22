package com.asterixorobelix.remit.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.asterixorobelix.remit.gallery.ComponentGallery
import com.github.takahirom.roborazzi.RoborazziOptions
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Phone-sized window in pixels (the test density is 1, so also in dp). */
private const val SCREEN_WIDTH = 360
private const val SCREEN_HEIGHT = 760

/**
 * Fail when more than 0.1% of the pixels differ (about 270 on this window). Re-renders on the same
 * OS are pixel-identical, so this only absorbs antialiasing noise; 0.5% would already let a
 * toggled checkbox plus a changed line of text (about 0.3%) pass.
 */
private const val CHANGE_THRESHOLD = 0.001f

private val screenshotOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD),
)

/**
 * Screenshot tests for shared UI, rendered offscreen at a fixed size in both themes.
 *
 * A plain `desktopTest` run only composes each screen (a smoke test). The Roborazzi tasks capture
 * the images: `recordRoborazziDesktop` writes the baselines to composeApp/screenshots/desktop/
 * (recorded on Linux CI, because fonts render differently on macOS), `verifyRoborazziDesktop`
 * fails on a missing or changed image, and `compareRoborazziDesktop` only reports differences.
 */
class ScreenshotTest {
    @Test
    fun componentGalleryLight() = snapshot("component_gallery_light") {
        ComponentGallery(useDarkTheme = false)
    }

    @Test
    fun componentGalleryDark() = snapshot("component_gallery_dark") {
        ComponentGallery(useDarkTheme = true)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun snapshot(name: String, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = SCREEN_WIDTH, height = SCREEN_HEIGHT) {
            setContent(content)
            waitForIdle()
            onRoot().captureRoboImage("$name.png", roborazziOptions = screenshotOptions)
        }
}
