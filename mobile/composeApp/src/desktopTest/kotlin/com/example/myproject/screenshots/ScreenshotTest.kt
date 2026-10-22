package com.example.myproject.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.example.myproject.domain.task.SyncStatus
import com.example.myproject.domain.task.Task
import com.example.myproject.gallery.ComponentGallery
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.auth_error_invalid_credentials
import com.example.myproject.presentation.auth.LoginContent
import com.example.myproject.presentation.auth.LoginForm
import com.example.myproject.presentation.auth.LoginUiState
import com.example.myproject.presentation.tasks.TasksContent
import com.example.myproject.presentation.tasks.TasksUiState
import com.example.myproject.ui.theme.AppTheme
import com.github.takahirom.roborazzi.RoborazziOptions
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Phone-sized window in pixels (the test density is 1, so also in dp). */
private const val SCREEN_WIDTH = 360
private const val SCREEN_HEIGHT = 760

/**
 * Fail when more than 0.1% of the pixels differ (about 270 on this window). Re-renders on the same
 * OS are pixel-identical, so this only absorbs antialiasing noise; 0.5% would already let a
 * toggled checkbox plus a changed task title (0.29%) pass.
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

    @Test
    fun tasksLight() = snapshot("tasks_light") {
        Themed(useDarkTheme = false) { TasksContent(state = fakeTasks, onEvent = {}) }
    }

    @Test
    fun tasksDark() = snapshot("tasks_dark") {
        Themed(useDarkTheme = true) { TasksContent(state = fakeTasks, onEvent = {}) }
    }

    @Test
    fun loginErrorLight() = snapshot("login_error_light") {
        Themed(useDarkTheme = false) { LoginContent(uiState = fakeLoginError, onEvent = {}, onNavigateToRegister = {}) }
    }

    @Test
    fun loginErrorDark() = snapshot("login_error_dark") {
        Themed(useDarkTheme = true) { LoginContent(uiState = fakeLoginError, onEvent = {}, onNavigateToRegister = {}) }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun snapshot(name: String, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = SCREEN_WIDTH, height = SCREEN_HEIGHT) {
            setContent(content)
            waitForIdle()
            onRoot().captureRoboImage("$name.png", roborazziOptions = screenshotOptions)
        }

    @Composable
    private fun Themed(useDarkTheme: Boolean, content: @Composable () -> Unit) {
        AppTheme(useDarkTheme = useDarkTheme) {
            Surface(Modifier.fillMaxSize()) { content() }
        }
    }

    private companion object {
        val fakeTasks = TasksUiState.Content(
            tasks = listOf(
                Task(id = "1", title = "Book flights", completed = false, updatedAt = 3, createdAt = 1),
                Task(id = "2", title = "Renew passport", completed = true, updatedAt = 2, createdAt = 1),
                Task(id = "3", title = "Pack the camera", completed = false, updatedAt = 1, createdAt = 1),
            ),
            syncStatus = SyncStatus.Pending(count = 2),
        )

        val fakeLoginError = LoginUiState.Error(
            form = LoginForm(email = "ada@example.com", password = "correct horse"),
            message = Res.string.auth_error_invalid_credentials,
        )
    }
}
