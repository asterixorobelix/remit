#!/usr/bin/env bash
# Scaffolds a local-first feature that compiles, passes detekt, and has a ViewModel test, then
# wires it into Koin, a typed route, the nav graph, and NavigationGraphTest (unless --no-wire).
# Usage: scripts/create-feature.sh PascalCaseName [--dry-run] [--no-wire]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
usage() { printf 'Usage: %s PascalCaseFeature [--dry-run] [--no-wire]\n' "$0" >&2; exit 2; }
NAME=""
DRY_RUN=""
WIRE=1
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=--dry-run ;;
    --no-wire) WIRE=0 ;;
    -*) printf 'Unknown option: %s\n' "$arg" >&2; exit 2 ;;
    *) [[ -z "$NAME" ]] || usage; NAME="$arg" ;;
  esac
done
[[ "$NAME" =~ ^[A-Z][A-Za-z0-9]+$ ]] || usage

PACKAGE="$(awk -F= '$1=="PACKAGE_NAME" { print $2 }' "$ROOT/app.properties")"
KEYWORDS=" as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while "
PKG_PATH="$(printf "%s" "$PACKAGE" | tr . /)"
SEGMENT="$(printf '%s' "$NAME" | tr '[:upper:]' '[:lower:]')"                 # package segment
KEY="$(printf '%s' "$NAME" | perl -pe 's/(?<!^)([A-Z])/_$1/g; $_ = lc')"         # resource prefix
CAMEL="$(printf '%s' "${NAME:0:1}" | tr '[:upper:]' '[:lower:]')${NAME:1}"          # Kotlin value names
WORDS="$(printf '%s' "$KEY" | tr '_' ' ')"
FEATURE_PKG="$PACKAGE.feature.$SEGMENT"
[[ "$KEYWORDS" != *" $SEGMENT "* ]] || { printf '%s would create the package segment "%s", a Kotlin keyword\n' "$NAME" "$SEGMENT" >&2; exit 2; }

SRC="$ROOT/mobile/composeApp/src"
BASE="$SRC/commonMain/kotlin/$PKG_PATH/feature/$SEGMENT"
TEST="$SRC/commonTest/kotlin/$PKG_PATH/feature/$SEGMENT"
SQL_DIR="$SRC/commonMain/sqldelight/$PKG_PATH/data/db"
SQL="$SQL_DIR/$NAME.sq"
SCHEMA_DIR="$SRC/commonMain/sqldelight/databases"
RESOURCES="$SRC/commonMain/composeResources"

# A shipped schema (databases/N.db) needs the new table in migration N.sqm; append to it
# when an unreleased migration already exists.
latest_schema=0
for db in "$SCHEMA_DIR"/*.db; do
  [[ -f "$db" ]] || continue
  version="$(basename "$db" .db)"
  [[ "$version" =~ ^[0-9]+$ ]] && ((version > latest_schema)) && latest_schema="$version"
done
MIGRATION=""
((latest_schema == 0)) || MIGRATION="$SQL_DIR/$latest_schema.sqm"

files=(
  "$BASE/${NAME}Contract.kt" "$BASE/${NAME}Repository.kt" "$BASE/${NAME}LocalRepository.kt"
  "$BASE/${NAME}UseCases.kt" "$BASE/${NAME}ViewModel.kt" "$BASE/${NAME}Screen.kt"
  "$BASE/${NAME}Module.kt" "$TEST/${NAME}ViewModelTest.kt" "$SQL"
)

# Wiring targets; each holds a `// create-feature:<anchor>` line that new code is inserted above.
APP_KT="$SRC/commonMain/kotlin/$PKG_PATH"
MODULES_KT="$APP_KT/di/AppModule.kt"
ROUTES_KT="$APP_KT/navigation/AppRoute.kt"
FEATURES_KT="$APP_KT/navigation/NavigationFeatures.kt"
NAV_KT="$APP_KT/navigation/AppNavigation.kt"
GRAPH_TEST_KT="$SRC/desktopTest/kotlin/$PKG_PATH/navigation/NavigationGraphTest.kt"
wiring=(
  "$MODULES_KT:modules" "$ROUTES_KT:routes" "$FEATURES_KT:available-routes" "$NAV_KT:destinations"
  "$GRAPH_TEST_KT:graph-routes" "$GRAPH_TEST_KT:graph-view-models"
)
has_anchor() { [[ -f "$1" ]] && grep -Eq "^[[:space:]]*// create-feature:$2\$" "$1"; }

# Generated names must not shadow existing declarations (for example Tasks -> TasksViewModel).
if [[ -d "$APP_KT" ]] && grep -rqE "(class|object|interface) ${NAME}(Route|ViewModel|UiState|Repository)?\b|val ${CAMEL}Module\b" \
  "$APP_KT"; then
  printf '%s clashes with an existing declaration; pick a more specific feature name\n' "$NAME" >&2
  exit 1
fi

if [[ "$DRY_RUN" == --dry-run ]]; then
  printf '%s\n' "${files[@]}"
  [[ -z "$MIGRATION" ]] || printf '%s (%s)\n' "$MIGRATION" "$([[ -f "$MIGRATION" ]] && echo append || echo create)"
  printf '%s/values*/strings.xml (append %s_* keys)\n' "$RESOURCES" "$KEY"
  if ((WIRE)); then
    for target in "${wiring[@]}"; do
      file="${target%:*}" anchor="${target##*:}"
      if has_anchor "$file" "$anchor"; then printf '%s (wire %s)\n' "$file" "$anchor"
      else printf '%s (skip %s: anchor missing, wire manually)\n' "$file" "$anchor"; fi
    done
  fi
  exit 0
fi
for file in "${files[@]}"; do [[ ! -e "$file" ]] || { printf 'Refusing to overwrite %s\n' "$file" >&2; exit 1; }; done
if grep -rqs "name=\"${KEY}_" "$RESOURCES"; then printf 'Refusing to overwrite existing %s_* strings\n' "$KEY" >&2; exit 1; fi
mkdir -p "$BASE" "$TEST" "$SQL_DIR"

cat > "$BASE/${NAME}Contract.kt" <<EOF
package $FEATURE_PKG

import org.jetbrains.compose.resources.StringResource

sealed interface ${NAME}UiState {
    data object Loading : ${NAME}UiState

    data class Content(
        val items: List<${NAME}Item>,
        val isRefreshing: Boolean = false,
    ) : ${NAME}UiState

    data class Empty(val isRefreshing: Boolean = false) : ${NAME}UiState

    data class Error(val message: StringResource) : ${NAME}UiState
}

sealed interface ${NAME}Event {
    data object Refresh : ${NAME}Event
}

sealed interface ${NAME}Effect {
    data class ShowMessage(val message: StringResource) : ${NAME}Effect
}
EOF

cat > "$BASE/${NAME}Repository.kt" <<EOF
package $FEATURE_PKG

import kotlinx.coroutines.flow.Flow

data class ${NAME}Item(val id: String, val title: String)

interface ${NAME}Repository {
    fun observe(): Flow<List<${NAME}Item>>

    suspend fun refresh(): Result<Unit>
}
EOF

cat > "$BASE/${NAME}LocalRepository.kt" <<EOF
package $FEATURE_PKG

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import $PACKAGE.data.db.AppDatabase
import $PACKAGE.platform.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Local-only repository over ${NAME}Entity; add a remote data source to sync. */
class ${NAME}LocalRepository(
    database: AppDatabase,
    private val dispatchers: AppDispatchers,
) : ${NAME}Repository {
    private val queries = database.${CAMEL}Queries

    override fun observe(): Flow<List<${NAME}Item>> =
        queries.selectAll().asFlow().mapToList(dispatchers.io).map { rows ->
            rows.map { row -> ${NAME}Item(id = row.id, title = row.title) }
        }

    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}
EOF

cat > "$BASE/${NAME}UseCases.kt" <<EOF
package $FEATURE_PKG

import kotlinx.coroutines.flow.Flow

class Observe${NAME}UseCase(private val repository: ${NAME}Repository) {
    operator fun invoke(): Flow<List<${NAME}Item>> = repository.observe()
}

class Refresh${NAME}UseCase(private val repository: ${NAME}Repository) {
    suspend operator fun invoke(): Result<Unit> = repository.refresh()
}
EOF

cat > "$BASE/${NAME}ViewModel.kt" <<EOF
package $FEATURE_PKG

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import $PACKAGE.generated.resources.Res
import $PACKAGE.generated.resources.${KEY}_error_load
import $PACKAGE.generated.resources.${KEY}_error_refresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long observing outlives the screen: enough for a configuration change, not the background. */
private const val STOP_TIMEOUT_MS = 5_000L

class ${NAME}ViewModel(
    private val observe${NAME}: Observe${NAME}UseCase,
    private val refresh${NAME}: Refresh${NAME}UseCase,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)
    private val reloadRequests = MutableStateFlow(0)

    /** Observes the data only while the screen is shown, so a hidden screen does no work. */
    val state: StateFlow<${NAME}UiState> = observeState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ${NAME}UiState.Loading)
    private val effectChannel = Channel<${NAME}Effect>(Channel.BUFFERED)
    val effects: Flow<${NAME}Effect> = effectChannel.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeState(): Flow<${NAME}UiState> = reloadRequests.flatMapLatest {
        combine(observe${NAME}(), refreshing) { items, isRefreshing ->
            if (items.isEmpty()) {
                ${NAME}UiState.Empty(isRefreshing)
            } else {
                ${NAME}UiState.Content(items, isRefreshing)
            }
        }.catch { emit(${NAME}UiState.Error(Res.string.${KEY}_error_load)) }
    }

    fun onEvent(event: ${NAME}Event) {
        when (event) {
            ${NAME}Event.Refresh -> refresh()
        }
    }

    private fun refresh() {
        if (state.value is ${NAME}UiState.Error) reloadRequests.value += 1
        viewModelScope.launch {
            refreshing.value = true
            refresh${NAME}().onFailure {
                effectChannel.send(${NAME}Effect.ShowMessage(Res.string.${KEY}_error_refresh))
            }
            refreshing.value = false
        }
    }
}
EOF

cat > "$BASE/${NAME}Screen.kt" <<EOF
package $FEATURE_PKG

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import $PACKAGE.generated.resources.Res
import $PACKAGE.generated.resources.cd_navigate_back
import $PACKAGE.generated.resources.${KEY}_empty
import $PACKAGE.generated.resources.${KEY}_error_load
import $PACKAGE.generated.resources.${KEY}_title
import $PACKAGE.presentation.common.CollectEffects
import $PACKAGE.ui.components.FullScreenError
import $PACKAGE.ui.preview.ScreenStates
import $PACKAGE.ui.theme.Dimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Navigation entry point for AppRoute.${NAME} (see navigation/AppNavigation.kt). */
@Composable
fun ${NAME}Route(
    onMessage: suspend (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ${NAME}ViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ${NAME}Effect.ShowMessage -> currentOnMessage(getString(effect.message))
        }
    }
    ${NAME}Screen(state = state, onEvent = viewModel::onEvent, onNavigateBack = onNavigateBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ${NAME}Screen(
    state: ${NAME}UiState,
    onEvent: (${NAME}Event) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.${KEY}_title)) },
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
    ) { innerPadding ->
        ${NAME}Content(state = state, onEvent = onEvent, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun ${NAME}Content(
    state: ${NAME}UiState,
    onEvent: (${NAME}Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ${NAME}UiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is ${NAME}UiState.Error -> FullScreenError(
            modifier = modifier,
            message = stringResource(state.message),
            onRetry = { onEvent(${NAME}Event.Refresh) },
        )

        is ${NAME}UiState.Empty -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.${KEY}_empty), style = MaterialTheme.typography.bodyLarge)
        }

        is ${NAME}UiState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
        ) {
            items(state.items, key = { it.id }) { item ->
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Every UiState side by side; replace the sample items with realistic data. */
@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ${NAME}ScreenPreview() {
    val items = listOf(
        ${NAME}Item(id = "1", title = "First"),
        ${NAME}Item(id = "2", title = "Second"),
    )
    val states = listOf(
        ${NAME}UiState.Loading,
        ${NAME}UiState.Empty(),
        ${NAME}UiState.Error(Res.string.${KEY}_error_load),
        ${NAME}UiState.Content(items),
    )
    ScreenStates(states) { state ->
        ${NAME}Screen(state = state, onEvent = {}, onNavigateBack = {})
    }
}
EOF

cat > "$BASE/${NAME}Module.kt" <<EOF
package $FEATURE_PKG

import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Loaded by appModules in di/AppModule.kt; gate it by a capability there if the feature is optional. */
val ${CAMEL}Module = module {
    singleOf(::${NAME}LocalRepository) bind ${NAME}Repository::class
    factoryOf(::Observe${NAME}UseCase)
    factoryOf(::Refresh${NAME}UseCase)
    viewModelOf(::${NAME}ViewModel)
}
EOF

cat > "$TEST/${NAME}ViewModelTest.kt" <<EOF
package $FEATURE_PKG

import app.cash.turbine.test
import $PACKAGE.generated.resources.Res
import $PACKAGE.generated.resources.${KEY}_error_refresh
import $PACKAGE.testing.awaitItemOfType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ${NAME}ViewModelTest {
    private class Fake${NAME}Repository : ${NAME}Repository {
        val items = MutableStateFlow<List<${NAME}Item>>(emptyList())
        var refreshResult: Result<Unit> = Result.success(Unit)

        override fun observe(): Flow<List<${NAME}Item>> = items

        override suspend fun refresh(): Result<Unit> = refreshResult
    }

    private val repository = Fake${NAME}Repository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ${NAME}ViewModel(
        observe${NAME} = Observe${NAME}UseCase(repository),
        refresh${NAME} = Refresh${NAME}UseCase(repository),
    )

    @Test
    fun emptyRepositoryShowsEmptyState() = runTest {
        viewModel().state.test {
            awaitItemOfType<${NAME}UiState.Empty>()
        }
    }

    @Test
    fun itemsShowContent() = runTest {
        val items = listOf(${NAME}Item(id = "1", title = "First"))
        repository.items.value = items

        viewModel().state.test {
            assertEquals(items, awaitItemOfType<${NAME}UiState.Content>().items)
        }
    }

    @Test
    fun refreshFailureShowsMessage() = runTest {
        repository.refreshResult = Result.failure(IllegalStateException("offline"))
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(${NAME}Event.Refresh)
            assertEquals(${NAME}Effect.ShowMessage(Res.string.${KEY}_error_refresh), awaitItem())
        }
    }
}
EOF

# ktlint orders imports lexicographically (ASCII) with java, javax, and kotlin last; the template
# order above depends on the package and resource names, so normalize every generated file.
for kotlin_file in "${files[@]}"; do
  [[ "$kotlin_file" == *.kt ]] || continue
  perl -0pi -e '
    s{((?:^import [^\n]+\n)+)}{
      my @lines = $1 =~ /^[^\n]+\n/mg;
      my $group = sub { $_[0] =~ /^import (?:java|javax|kotlin)\./ ? 1 : 0 };
      join "", sort { $group->($a) <=> $group->($b) || $a cmp $b } @lines
    }me
  ' "$kotlin_file"
done

TABLE_SQL="CREATE TABLE ${NAME}Entity (
    id TEXT NOT NULL PRIMARY KEY,
    title TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);"

cat > "$SQL" <<EOF
$TABLE_SQL

selectAll:
SELECT * FROM ${NAME}Entity ORDER BY updated_at DESC;

upsert:
INSERT OR REPLACE INTO ${NAME}Entity(id, title, updated_at) VALUES (?, ?, ?);

deleteById:
DELETE FROM ${NAME}Entity WHERE id = ?;
EOF

if [[ -n "$MIGRATION" ]]; then
  separator=""
  [[ -s "$MIGRATION" ]] && separator=$'\n'
  printf '%s%s\n' "$separator" "$TABLE_SQL" >> "$MIGRATION"
fi

# English copy in every locale; translate the non-default ones.
for strings in "$RESOURCES"/values*/strings.xml; do
  [[ -f "$strings" ]] || continue
  KEY="$KEY" NAME="$NAME" WORDS="$WORDS" perl -0pi -e '
    my $block = "\n\n    <!-- $ENV{NAME} -->\n"
      . "    <string name=\"$ENV{KEY}_title\">$ENV{NAME}</string>\n"
      . "    <string name=\"$ENV{KEY}_empty\">No $ENV{WORDS} yet</string>\n"
      . "    <string name=\"$ENV{KEY}_error_load\">Could not load $ENV{WORDS}</string>\n"
      . "    <string name=\"$ENV{KEY}_error_refresh\">Could not refresh $ENV{WORDS}</string>\n";
    s{\s*</resources>\s*\z}{$block</resources>\n};
  ' "$strings"
done

# Inserts TEXT above the anchor line in FILE.
insert_at_anchor() { # FILE ANCHOR TEXT
  ANCHOR="$2" TEXT="$3" perl -0pi -e 's{^([ \t]*// create-feature:\Q$ENV{ANCHOR}\E\n)}{$ENV{TEXT}$1}m' "$1"
}
# Adds an import in sorted position (java/javax/kotlin imports last, as ktlint orders them).
add_import() { # FILE FQN
  IMPORT="import $2" perl -i -ne '
    BEGIN { $new = $ENV{IMPORT}; $group = sub { $_[0] =~ /^import (java|javax|kotlin)\./ ? 1 : 0 } }
    if (!$done && /^import / && $group->($_) == $group->($new) && $_ gt "$new\n") { print "$new\n"; $done = 1 }
    if (!$done && /^import / && $group->($_) > $group->($new)) { print "$new\n"; $done = 1 }
    $seen_import = 1 if /^import /;
    if (!$done && $seen_import && !/^import / ) { print "$new\n"; $done = 1 }
    print;
  ' "$1"
}

manual=()
wire() { # FILE ANCHOR TEXT [IMPORT]
  if has_anchor "$1" "$2"; then
    insert_at_anchor "$1" "$2" "$3"
    [[ -z "${4:-}" ]] || grep -qxF "import $4" "$1" || add_import "$1" "$4"
  else
    manual+=("$(basename "$1"): no '// create-feature:$2' anchor")
  fi
}

if ((WIRE)); then
  wire "$MODULES_KT" modules "    add(${CAMEL}Module)"$'\n' "$FEATURE_PKG.${CAMEL}Module"
  wire "$ROUTES_KT" routes $'\n'"    @Serializable"$'\n'"    data object ${NAME} : AppRoute"$'\n'
  wire "$FEATURES_KT" available-routes "    AppRoute.${NAME},"$'\n'
  wire "$NAV_KT" destinations "    composable<AppRoute.${NAME}> {
        ${NAME}Route(
            onMessage = { message -> snackbarHostState.showSnackbar(message) },
            onNavigateBack = { navController.popBackStack() },
        )
    }
" "$FEATURE_PKG.${NAME}Route"
  wire "$GRAPH_TEST_KT" graph-routes "        AppRoute.${NAME},"$'\n'
  wire "$GRAPH_TEST_KT" graph-view-models "        AppRoute.${NAME} -> listOf(${NAME}ViewModel::class)"$'\n' \
    "$FEATURE_PKG.${NAME}ViewModel"
fi

printf 'Created %s in %s%s.\n' "$NAME" "$FEATURE_PKG" "${MIGRATION:+ (table added to migration $(basename "$MIGRATION"))}"
step=1
next() { printf '  %s. %s\n' "$step" "$1"; step=$((step + 1)); }
printf 'Next:\n'
if ((WIRE)); then
  printf '  Wired %sModule, AppRoute.%s, its destination, and NavigationGraphTest.\n' "$CAMEL" "$NAME"
  for problem in "${manual[@]+"${manual[@]}"}"; do printf '  Not wired (%s); do it by hand.\n' "$problem"; done
  next "Link to it: navController.navigate(AppRoute.${NAME}) from Home, Settings, or a tab."
  next "Gate it by capability (appModules + NavigationFeatures) if the feature is optional."
else
  next "Register ${CAMEL}Module in di/AppModule.kt (gate it by capability if optional)."
  next "Add a typed route that shows ${NAME}Route, gate it in NavigationFeatures, and cover it in NavigationGraphTest."
fi
next "Replace the placeholder item model and UI; add a remote data source if it syncs."
next "Run: make quick (then make check-mobile before pushing)"
