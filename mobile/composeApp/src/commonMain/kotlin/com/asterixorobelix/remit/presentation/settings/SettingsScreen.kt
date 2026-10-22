package com.asterixorobelix.remit.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.domain.settings.ThemeMode
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.settings_about
import com.asterixorobelix.remit.generated.resources.settings_appearance
import com.asterixorobelix.remit.generated.resources.settings_developer
import com.asterixorobelix.remit.generated.resources.settings_feedback
import com.asterixorobelix.remit.generated.resources.settings_privacy_policy
import com.asterixorobelix.remit.generated.resources.settings_privacy_url
import com.asterixorobelix.remit.generated.resources.settings_rate_app
import com.asterixorobelix.remit.generated.resources.settings_terms
import com.asterixorobelix.remit.generated.resources.settings_terms_url
import com.asterixorobelix.remit.generated.resources.settings_theme_dark
import com.asterixorobelix.remit.generated.resources.settings_theme_light
import com.asterixorobelix.remit.generated.resources.settings_theme_system
import com.asterixorobelix.remit.generated.resources.settings_version
import com.asterixorobelix.remit.platform.RequestInAppReview
import com.asterixorobelix.remit.ui.components.SectionHeader
import com.asterixorobelix.remit.ui.preview.ScreenStates
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Navigation callbacks for Settings; Developer tools are only shown in debug builds. */
data class SettingsNavigation(
    val toFeedback: () -> Unit,
    val toWebView: (url: String, title: String) -> Unit,
    val toDeveloper: () -> Unit,
)

@Composable
fun SettingsScreen(
    navigation: SettingsNavigation,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var requestReview by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestReview -> requestReview = true
            }
        }
    }
    RequestInAppReview(
        shouldRequest = requestReview,
        onReviewFlowFinish = {
            requestReview = false
            viewModel.onEvent(SettingsEvent.ReviewFlowFinished)
        },
    )

    when (val state = uiState) {
        SettingsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is SettingsUiState.Content -> SettingsContent(state, viewModel::onEvent, navigation, modifier)
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Content,
    onEvent: (SettingsEvent) -> Unit,
    navigation: SettingsNavigation,
    modifier: Modifier = Modifier,
) {
    val privacyUrl = stringResource(Res.string.settings_privacy_url)
    val privacyTitle = stringResource(Res.string.settings_privacy_policy)
    val termsUrl = stringResource(Res.string.settings_terms_url)
    val termsTitle = stringResource(Res.string.settings_terms)

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionHeader(text = stringResource(Res.string.settings_appearance))
        ThemeOption(Res.string.settings_theme_system, Icons.Outlined.PhoneAndroid, ThemeMode.SYSTEM, state, onEvent)
        ThemeOption(Res.string.settings_theme_light, Icons.Outlined.LightMode, ThemeMode.LIGHT, state, onEvent)
        ThemeOption(Res.string.settings_theme_dark, Icons.Outlined.DarkMode, ThemeMode.DARK, state, onEvent)
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))

        SectionHeader(text = stringResource(Res.string.settings_about))
        SettingsLink(Res.string.settings_feedback, Icons.AutoMirrored.Outlined.Chat, navigation.toFeedback)
        SettingsLink(Res.string.settings_rate_app, Icons.Outlined.Star) { onEvent(SettingsEvent.RateAppClicked) }
        SettingsLink(Res.string.settings_privacy_policy, Icons.Outlined.PrivacyTip) {
            navigation.toWebView(privacyUrl, privacyTitle)
        }
        SettingsLink(Res.string.settings_terms, Icons.Outlined.Description) {
            navigation.toWebView(termsUrl, termsTitle)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.paddingSmall))
        ListItem(
            headlineContent = { Text(stringResource(Res.string.settings_version)) },
            supportingContent = {
                Text(
                    text = state.appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
        )
        if (state.showDeveloperTools) {
            SettingsLink(Res.string.settings_developer, Icons.Outlined.DeveloperMode, navigation.toDeveloper)
        }
        Spacer(modifier = Modifier.height(Dimens.spacingSection))
    }
}

@Composable
private fun SettingsLink(label: StringResource, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun ThemeOption(
    label: StringResource,
    icon: ImageVector,
    mode: ThemeMode,
    state: SettingsUiState.Content,
    onEvent: (SettingsEvent) -> Unit,
) {
    val selected = state.themeMode == mode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton) {
                onEvent(SettingsEvent.ThemeModeSelected(mode))
            }
            .padding(horizontal = Dimens.paddingMedium, vertical = Dimens.paddingCompact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(Dimens.spacingMedium))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = null)
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun SettingsContentPreview() {
    val navigation = SettingsNavigation(toFeedback = {}, toWebView = { _, _ -> }, toDeveloper = {})
    val states = listOf(
        SettingsUiState.Content(ThemeMode.SYSTEM, appVersion = "1.0.0 (10000)", showDeveloperTools = true),
        SettingsUiState.Content(ThemeMode.DARK, appVersion = "1.0.0 (10000)", showDeveloperTools = false),
    )
    ScreenStates(states) { state -> SettingsContent(state = state, onEvent = {}, navigation = navigation) }
}
