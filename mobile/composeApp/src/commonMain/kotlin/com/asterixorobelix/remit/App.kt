package com.asterixorobelix.remit

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.asterixorobelix.remit.domain.settings.ThemeMode
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.app_store_id
import com.asterixorobelix.remit.navigation.AppNavigation
import com.asterixorobelix.remit.platform.RequestInAppReview
import com.asterixorobelix.remit.platform.StoreListing
import com.asterixorobelix.remit.platform.openStoreListing
import com.asterixorobelix.remit.presentation.app.AppEffect
import com.asterixorobelix.remit.presentation.app.AppUiState
import com.asterixorobelix.remit.presentation.app.AppViewModel
import com.asterixorobelix.remit.presentation.forceupdate.ForceUpdateScreen
import com.asterixorobelix.remit.ui.theme.AppTheme
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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                AppEffect.RequestReview -> requestReview = true
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
                    modifier = modifier,
                )
            }
        }
    }
}
