package com.example.myproject.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myproject.domain.home.DayPeriod
import com.example.myproject.domain.task.Task
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.home_action_map
import com.example.myproject.generated.resources.home_action_premium
import com.example.myproject.generated.resources.home_action_profile
import com.example.myproject.generated.resources.home_action_search
import com.example.myproject.generated.resources.home_action_settings
import com.example.myproject.generated.resources.home_action_tasks
import com.example.myproject.generated.resources.home_featured_subtitle_search
import com.example.myproject.generated.resources.home_featured_subtitle_tasks
import com.example.myproject.generated.resources.home_featured_title_search
import com.example.myproject.generated.resources.home_featured_title_tasks
import com.example.myproject.generated.resources.home_greeting_afternoon
import com.example.myproject.generated.resources.home_greeting_evening
import com.example.myproject.generated.resources.home_greeting_morning
import com.example.myproject.generated.resources.home_quick_actions
import com.example.myproject.generated.resources.home_recent_tasks
import com.example.myproject.generated.resources.home_recent_tasks_empty
import com.example.myproject.ui.components.AppCard
import com.example.myproject.ui.components.PrimaryButton
import com.example.myproject.ui.theme.Dimens
import com.example.myproject.ui.theme.pressedScale
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val STAGGER_DELAY_MS = 80L
private const val HEADER_SLIDE_DIVISOR = 2
private const val ITEM_SLIDE_DIVISOR = 3

@Composable
fun HomeScreen(
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        HomeUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is HomeUiState.Content -> HomeContent(state, onAction, modifier)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Dimens.spacingLarge),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / HEADER_SLIDE_DIVISOR },
        ) {
            GreetingHeader(displayName = state.displayName, dayPeriod = state.dayPeriod)
        }
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        StaggeredItem(visible = visible, index = 1) {
            FeaturedCard(action = state.featuredAction, onAction = onAction)
        }
        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
        StaggeredItem(visible = visible, index = 2) {
            SectionTitle(text = stringResource(Res.string.home_quick_actions))
        }
        Spacer(modifier = Modifier.height(Dimens.spacingCompact))
        StaggeredItem(visible = visible, index = 3) {
            QuickActionsRow(actions = state.quickActions, onAction = onAction)
        }
        val recentTasks = state.recentTasks
        if (recentTasks != null) {
            Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
            StaggeredItem(visible = visible, index = 4) {
                RecentTasksSection(tasks = recentTasks, onOpenTasks = { onAction(HomeAction.TASKS) })
            }
        }
        Spacer(modifier = Modifier.height(Dimens.spacingSection))
    }
}

@Composable
private fun StaggeredItem(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(index * STAGGER_DELAY_MS)
            itemVisible = true
        }
    }
    AnimatedVisibility(
        visible = itemVisible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
            slideInVertically(spring(stiffness = Spring.StiffnessLow)) { it / ITEM_SLIDE_DIVISOR },
    ) {
        content()
    }
}

@Composable
private fun GreetingHeader(displayName: String, dayPeriod: DayPeriod) {
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (displayName.isNotEmpty()) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.WbSunny,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeLarge),
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun FeaturedCard(action: HomeAction, onAction: (HomeAction) -> Unit) {
    val (title, subtitle) = if (action == HomeAction.TASKS) {
        Res.string.home_featured_title_tasks to Res.string.home_featured_subtitle_tasks
    } else {
        Res.string.home_featured_title_search to Res.string.home_featured_subtitle_search
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            )
            .padding(Dimens.paddingLarge),
    ) {
        Column {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            PrimaryButton(
                onClick = { onAction(action) },
                text = stringResource(action.label()),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding),
    )
}

@Composable
private fun QuickActionsRow(actions: List<HomeAction>, onAction: (HomeAction) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.screenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingCompact),
    ) {
        items(actions) { action ->
            QuickActionChip(action = action, onClick = { onAction(action) })
        }
    }
}

@Composable
private fun QuickActionChip(action: HomeAction, onClick: () -> Unit) {
    val label = stringResource(action.label())
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pressedScale()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Dimens.paddingSmall),
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.touchTargetComfortable)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentTasksSection(tasks: List<Task>, onOpenTasks: () -> Unit) {
    Column {
        SectionTitle(text = stringResource(Res.string.home_recent_tasks))
        Spacer(modifier = Modifier.height(Dimens.spacingCompact))
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(Res.string.home_recent_tasks_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding),
            )
        }
        tasks.forEach { task -> RecentTaskItem(task = task, onClick = onOpenTasks) }
    }
}

@Composable
private fun RecentTaskItem(task: Task, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenHorizontalPadding, vertical = Dimens.spacingExtraSmall),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSizeMedium),
            )
            Spacer(modifier = Modifier.width(Dimens.spacingCompact))
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun HomeAction.label(): StringResource = when (this) {
    HomeAction.TASKS -> Res.string.home_action_tasks
    HomeAction.SEARCH -> Res.string.home_action_search
    HomeAction.PROFILE -> Res.string.home_action_profile
    HomeAction.SETTINGS -> Res.string.home_action_settings
    HomeAction.MAP -> Res.string.home_action_map
    HomeAction.PREMIUM -> Res.string.home_action_premium
}

private fun HomeAction.icon(): ImageVector = when (this) {
    HomeAction.TASKS -> Icons.Outlined.Checklist
    HomeAction.SEARCH -> Icons.Outlined.Search
    HomeAction.PROFILE -> Icons.Outlined.Person
    HomeAction.SETTINGS -> Icons.Outlined.Settings
    HomeAction.MAP -> Icons.Outlined.Map
    HomeAction.PREMIUM -> Icons.Outlined.WorkspacePremium
}
