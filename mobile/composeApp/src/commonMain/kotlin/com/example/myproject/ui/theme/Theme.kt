package com.example.myproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Application theme wrapper that sets up Material Design 3 theming.
 *
 * This theme should ONLY be applied at the root level in App.kt.
 * Individual screens and components should NOT wrap themselves in AppTheme,
 * as they inherit the theme automatically from the composition hierarchy.
 *
 * Features:
 * - Automatic light/dark theme switching based on system settings
 * - Full Material Design 3 color scheme with semantic tokens
 * - Consistent typography scale
 * - Standard shape system
 *
 * Usage in App.kt:
 * ```kotlin
 * @Composable
 * fun App() {
 *     AppTheme {
 *         // Your app content here
 *         MyScreen()
 *     }
 * }
 * ```
 *
 * Usage in screens (NO wrapping needed):
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     Column(
 *         modifier = Modifier.background(MaterialTheme.colorScheme.background)
 *     ) {
 *         Text(
 *             text = "Hello",
 *             style = MaterialTheme.typography.headlineMedium,
 *             color = MaterialTheme.colorScheme.onBackground
 *         )
 *     }
 * }
 * ```
 *
 * Usage in Preview functions (wrapping IS needed):
 * ```kotlin
 * @Preview
 * @Composable
 * private fun MyComponentPreview() {
 *     AppTheme {
 *         MyComponent()
 *     }
 * }
 * ```
 *
 * @param useDarkTheme Whether to use dark theme. Defaults to system setting.
 * @param content The composable content to be themed.
 */
@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
