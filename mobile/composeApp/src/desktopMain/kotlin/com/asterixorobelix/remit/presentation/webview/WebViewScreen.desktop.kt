package com.asterixorobelix.remit.presentation.webview

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable actual fun WebViewScreen(url: String, title: String, onNavigateBack: () -> Unit, modifier: Modifier) {
    Text("$title: $url", modifier = modifier)
}
