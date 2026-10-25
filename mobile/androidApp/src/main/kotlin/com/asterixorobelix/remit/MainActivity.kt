package com.asterixorobelix.remit

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.presentation.app.AppUiState
import com.asterixorobelix.remit.presentation.app.AppViewModel
import com.asterixorobelix.remit.ui.theme.isDarkTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    // testTagsAsResourceId exposes Compose test tags as resource ids, so UI Automator and the
    // Appium tests in e2e/ can find elements such as send_amount and history_item.
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // The splash screen stays until the start destination and the saved theme have been read,
        // so the first frame is the right screen in the right theme.
        installSplashScreen().setKeepOnScreenCondition { appViewModel.uiState.value == AppUiState.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = isDarkTheme(themeMode)
            // enableEdgeToEdge picks the bar icon colours from the system setting, which the
            // Settings theme can override: keep the icons readable on the app's own background.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            App(
                modifier = Modifier.semantics { testTagsAsResourceId = true },
                viewModel = appViewModel,
            )
        }
    }

    private companion object {
        // The scrims enableEdgeToEdge uses by default for three-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
