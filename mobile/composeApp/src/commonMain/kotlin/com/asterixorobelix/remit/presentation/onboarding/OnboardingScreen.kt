package com.asterixorobelix.remit.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.button_next
import com.asterixorobelix.remit.generated.resources.onboarding_desc_1
import com.asterixorobelix.remit.generated.resources.onboarding_desc_2
import com.asterixorobelix.remit.generated.resources.onboarding_desc_3
import com.asterixorobelix.remit.generated.resources.onboarding_get_started
import com.asterixorobelix.remit.generated.resources.onboarding_skip
import com.asterixorobelix.remit.generated.resources.onboarding_title_1
import com.asterixorobelix.remit.generated.resources.onboarding_title_2
import com.asterixorobelix.remit.generated.resources.onboarding_title_3
import com.asterixorobelix.remit.presentation.common.CollectEffects
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.preview.ScreenStates
import com.asterixorobelix.remit.ui.theme.Dimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val ONBOARDING_PAGE_COUNT = 3
private const val INDICATOR_DAMPING_RATIO = 0.8f

private data class OnboardingPage(
    val icon: ImageVector,
    val title: StringResource,
    val description: StringResource,
)

private val pages = listOf(
    OnboardingPage(Icons.AutoMirrored.Outlined.Send, Res.string.onboarding_title_1, Res.string.onboarding_desc_1),
    OnboardingPage(Icons.Outlined.History, Res.string.onboarding_title_2, Res.string.onboarding_desc_2),
    OnboardingPage(Icons.Outlined.VerifiedUser, Res.string.onboarding_title_3, Res.string.onboarding_desc_3),
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val currentOnComplete by rememberUpdatedState(onComplete)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.Completed -> currentOnComplete()
        }
    }
    OnboardingContent(onFinish = viewModel::onFinish, modifier = modifier)
}

/** The pager and its controls; [onFinish] is Skip, or Get started on the last page. */
@Composable
private fun OnboardingContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { ONBOARDING_PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1

    val completeOnboarding = onFinish

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Dimens.paddingMedium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = completeOnboarding) {
                Text(stringResource(Res.string.onboarding_skip))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            PageContent(page = pages[pageIndex])
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.paddingMedium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(ONBOARDING_PAGE_COUNT) { index ->
                val isSelected = index == pagerState.currentPage
                val color by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "indicatorColor",
                )
                val width by animateDpAsState(
                    targetValue = if (isSelected) Dimens.spacingLarge else Dimens.spacingSmall,
                    animationSpec = spring(dampingRatio = INDICATOR_DAMPING_RATIO),
                    label = "indicatorWidth",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = Dimens.spacingExtraSmall)
                        .height(Dimens.spacingSmall)
                        .width(width)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        PrimaryButton(
            onClick = {
                if (isLastPage) {
                    completeOnboarding()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            text = if (isLastPage) {
                stringResource(Res.string.onboarding_get_started)
            } else {
                stringResource(Res.string.button_next)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeDisplayLarge),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        Text(
            text = stringResource(page.title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.paddingLarge),
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        Text(
            text = stringResource(page.description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.paddingExtraLarge),
        )
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun OnboardingContentPreview() {
    ScreenStates((0 until ONBOARDING_PAGE_COUNT).toList()) { page ->
        OnboardingContent(onFinish = {}, initialPage = page)
    }
}
