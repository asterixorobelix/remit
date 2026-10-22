package com.example.myproject

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.myproject.domain.settings.ThemeMode
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.app_store_id
import com.example.myproject.navigation.AppNavigation
import com.example.myproject.platform.RequestInAppReview
import com.example.myproject.platform.StoreListing
import com.example.myproject.platform.openStoreListing
import com.example.myproject.presentation.app.AppEffect
import com.example.myproject.presentation.app.AppUiState
import com.example.myproject.presentation.app.AppViewModel
import com.example.myproject.presentation.forceupdate.ForceUpdateScreen
import com.example.myproject.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = koinViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var requestReview by remember { mutableStateOf(false) }
    var sessionExpiredSignal by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AppEffect.RequestReview -> requestReview = true
                AppEffect.SessionExpired -> sessionExpiredSignal += 1
            }
        }
    }

    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        RequestInAppReview(
            shouldRequest = requestReview,
            onReviewFlowFinish = {
                requestReview = false
                viewModel.onReviewFlowFinished()
            },
        )
        when (val state = uiState) {
            AppUiState.Loading -> Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))

            is AppUiState.Ready -> if (state.updateRequired) {
                val listing = StoreListing(
                    androidPackage = viewModel.applicationId,
                    appStoreId = stringResource(Res.string.app_store_id),
                )
                ForceUpdateScreen(onUpdateClick = { openStoreListing(listing) }, modifier = modifier)
            } else {
                AppNavigation(
                    navController = rememberNavController(),
                    startDestination = state.startDestination,
                    features = viewModel.navigationFeatures,
                    sessionExpiredSignal = sessionExpiredSignal,
                    modifier = modifier,
                )
            }
        }
    }
}
