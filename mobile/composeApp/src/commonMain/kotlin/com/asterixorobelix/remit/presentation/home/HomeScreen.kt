package com.asterixorobelix.remit.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.domain.home.DayPeriod
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.home_empty_description
import com.asterixorobelix.remit.generated.resources.home_empty_title
import com.asterixorobelix.remit.generated.resources.home_greeting_afternoon
import com.asterixorobelix.remit.generated.resources.home_greeting_evening
import com.asterixorobelix.remit.generated.resources.home_greeting_morning
import com.asterixorobelix.remit.ui.components.EmptyState
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is HomeUiState.Content -> HomeContent(state, modifier)
    }
}

@Composable
private fun HomeContent(state: HomeUiState.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Dimens.spacingLarge),
    ) {
        GreetingHeader(dayPeriod = state.dayPeriod)
        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
        EmptyState(
            title = stringResource(Res.string.home_empty_title),
            description = stringResource(Res.string.home_empty_description),
        )
    }
}

@Composable
private fun GreetingHeader(dayPeriod: DayPeriod) {
    val greeting = when (dayPeriod) {
        DayPeriod.MORNING -> stringResource(Res.string.home_greeting_morning)
        DayPeriod.AFTERNOON -> stringResource(Res.string.home_greeting_afternoon)
        DayPeriod.EVENING -> stringResource(Res.string.home_greeting_evening)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeLarge),
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}
