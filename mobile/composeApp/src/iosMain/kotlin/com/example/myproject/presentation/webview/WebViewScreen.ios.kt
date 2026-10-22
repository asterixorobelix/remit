@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.myproject.presentation.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_navigate_back
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun WebViewScreen(
    url: String,
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        UIKitView(
            factory = {
                val webView = WKWebView()
                val nsUrl = NSURL.URLWithString(url)
                if (nsUrl != null) {
                    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
                webView
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
