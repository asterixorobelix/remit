# Codebase Concerns

**Analysis Date:** 2026-01-15

## Tech Debt

**Minimal Test Coverage in Mobile:**
- Issue: Only trivial test exists (`assertEquals(3, 1 + 2)`)
- Files: `mobile/composeApp/src/commonTest/kotlin/com/example/myproject/ComposeAppCommonTest.kt`
- Why: Template project - tests not yet implemented
- Impact: No verification of business logic, repositories, or ViewModels
- Fix approach: Add unit tests for `UserRepository`, `UserViewModel`, `UserApi`

**Cat API Example Code:**
- Issue: Demo API code (`CatAsAServiceApi`, `KatApi`) mixed with production patterns
- Files: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/CatAsAServiceApi.kt`, `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/KatApi.kt`
- Why: Template demonstrates API usage
- Impact: May confuse as "real" feature when it's example code
- Fix approach: Move to separate `examples/` package or remove when building real app

## Known Bugs

**Unsafe Type Cast in CatAsAServiceApi:**
- Symptoms: `ClassCastException` if API response format changes
- Trigger: Call `getCat()` when API returns unexpected JSON structure
- File: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/CatAsAServiceApi.kt`
- Workaround: None - app will crash
- Root cause: Line 10 uses `response.body() as Cat` instead of safe deserialization
- Fix: Replace with `response.body<Cat>()` or wrap in `runCatching {}`

**Missing Error Handling in AppViewModel:**
- Symptoms: App crash if network request fails
- Trigger: Call `getCat()` with no network or API unavailable
- File: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/AppViewModel.kt`
- Workaround: Ensure network connectivity before use
- Root cause: No try-catch or Result handling around API call
- Fix: Add error handling and update UI state on failure

## Security Considerations

**Default JWT Secret:**
- Risk: If production uses default `"change-me-in-production"`, all tokens vulnerable
- File: `backend/src/main/kotlin/com/example/myproject/configuration/AppConfig.kt`
- Current mitigation: Clearly documented warning in code and .env.example
- Recommendations: Add runtime check that fails startup if default secret used in production

**CORS Configuration:**
- Risk: Misconfigured CORS could allow unauthorized origins
- File: `backend/src/main/kotlin/com/example/myproject/plugins/HTTP.kt`
- Current mitigation: Defaults to empty list in production (most restrictive)
- Recommendations: Document required CORS setup in deployment guide

## Performance Bottlenecks

**No Pagination in User List API:**
- Problem: `GET /api/users` fetches all users without limit
- File: `backend/src/main/kotlin/com/example/myproject/domain/user/UserRoutes.kt`
- Measurement: Will degrade as user count grows (O(n) response size)
- Cause: No limit/offset parameters implemented
- Improvement path: Add `page` and `limit` query parameters with defaults

**No Cache TTL in Mobile Repository:**
- Problem: In-memory cache has no expiration
- File: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/data/user/UserRepository.kt`
- Measurement: Stale data if app stays open for hours
- Cause: StateFlow caches indefinitely
- Improvement path: Add cache TTL (e.g., 5 minutes) with refresh mechanism

## Fragile Areas

**Platform Abstraction Implementations:**
- Files: `mobile/composeApp/src/androidMain/kotlin/.../platform/`, `mobile/composeApp/src/iosMain/kotlin/.../platform/`
- Why fragile: expect/actual pattern requires matching signatures across platforms
- Common failures: Adding new method to interface but forgetting iOS implementation
- Safe modification: Always update both androidMain and iosMain simultaneously
- Test coverage: No tests for platform implementations

**Koin Module Configuration:**
- File: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/di/AppModule.kt`
- Why fragile: Missing dependency registration causes runtime crash, not compile error
- Common failures: Adding new class but forgetting Koin registration
- Safe modification: Test app startup after any DI changes
- Test coverage: No DI integration tests

## Scaling Limits

**In-Memory Database (Development):**
- Current capacity: Single developer local testing
- Limit: Data lost on restart, no concurrent access
- Symptoms at limit: Data loss, conflicts in team development
- Scaling path: Already supports PostgreSQL for production

**GitHub Actions Free Tier:**
- Current capacity: 2,000 minutes/month
- Limit: May exceed with frequent PRs
- Symptoms at limit: Builds queued or blocked
- Scaling path: GitHub Teams plan or self-hosted runners

## Dependencies at Risk

**No Critical Risks Detected:**
- All major dependencies (Kotlin, Ktor, Compose) actively maintained
- Firebase dependencies are optional with fallback
- Version catalog (`libs.versions.toml`) uses recent versions

**Watch List:**
- Kamel 1.0.8 - Image loading library, monitor for updates
- Koin 4.1.1 - DI framework, stable but watch for KMP compatibility

## Missing Critical Features

**Mobile Authentication UI:**
- Problem: No login/registration screens implemented
- Current workaround: Backend JWT works, but mobile can't obtain tokens
- Blocks: Protected API endpoints unusable from mobile
- Implementation complexity: Medium (add auth screens, token storage)

**Offline Support:**
- Problem: App requires network connectivity
- Current workaround: Show error states on network failure
- Blocks: Offline-first user experience
- Implementation complexity: High (local database, sync logic)

## Test Coverage Gaps

**User CRUD Endpoints:**
- What's not tested: `/api/users` POST, PUT, DELETE operations
- Files: `backend/src/main/kotlin/com/example/myproject/domain/user/UserRoutes.kt`
- Risk: Regressions in user management undetected
- Priority: High
- Difficulty: Low - follow existing `ApplicationTest.kt` patterns

**Mobile Data Layer:**
- What's not tested: `UserRepository`, `UserApi`, `UserViewModel`
- Files: `mobile/composeApp/src/commonMain/kotlin/.../data/user/`, `mobile/composeApp/src/commonMain/kotlin/.../presentation/user/`
- Risk: Business logic bugs undetected
- Priority: High
- Difficulty: Medium - need to set up mocking framework

**Error States:**
- What's not tested: Network failure handling, API error responses
- Risk: Poor user experience on failures
- Priority: Medium
- Difficulty: Medium - need to mock failure scenarios

---

## CMP/KMM Best Practices (Learnings from Production Projects)

These patterns have been validated in production Kotlin Multiplatform projects.

### Firebase Configuration Detection

**Pattern**: Detect whether Firebase is using a placeholder config vs. real credentials.

```kotlin
// In platform/FirebaseConfigChecker.kt
object FirebaseConfigChecker {
    /**
     * Returns true if the app is using the default template google-services.json
     * rather than a real Firebase configuration.
     *
     * When true, Firebase services (Crashlytics, Analytics) should be disabled
     * to avoid crashes and misleading data.
     */
    fun isUsingDefaultTemplate(): Boolean {
        // Check for template markers in google-services.json
        // e.g., project_id contains "example" or "template"
        return BuildConfig.FIREBASE_PROJECT_ID.contains("example") ||
               BuildConfig.FIREBASE_PROJECT_ID.isEmpty()
    }
}

// Usage in CrashLogger/Analytics initialization:
if (!FirebaseConfigChecker.isUsingDefaultTemplate()) {
    FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
}
```

**Why**: Prevents crashes and misleading analytics when Firebase isn't properly configured.

---

### Cache TTL Implementation

**Pattern**: In-memory caches should have time-to-live and size limits.

```kotlin
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isExpired(ttlMillis: Long): Boolean =
        System.currentTimeMillis() - timestamp > ttlMillis
}

class CachedRepository(
    private val api: Api,
    private val cacheTtlMillis: Long = 5 * 60 * 1000L, // 5 minutes
    private val maxCacheSize: Int = 100
) {
    private val cache = mutableMapOf<String, CacheEntry<Data>>()
    private val mutex = Mutex()

    suspend fun getData(id: String): Data {
        mutex.withLock {
            val entry = cache[id]
            if (entry != null && !entry.isExpired(cacheTtlMillis)) {
                return entry.data
            }
        }

        val freshData = api.fetchData(id)

        mutex.withLock {
            // Evict oldest entries if cache is full
            if (cache.size >= maxCacheSize) {
                val oldest = cache.entries.minByOrNull { it.value.timestamp }
                oldest?.let { cache.remove(it.key) }
            }
            cache[id] = CacheEntry(freshData)
        }

        return freshData
    }
}
```

**Why**: Prevents unbounded memory growth in long-running sessions and ensures data freshness.

---

### UI Component Size Limits

**Guideline**: Keep Composable files under 250 lines.

**Warning Signs**:
- Composable file exceeds 250 lines
- Multiple unrelated UI sections in one file
- Hard to find specific UI elements
- Difficult to write focused tests

**Fix Approach**:
1. Extract sub-components (e.g., `UserCard`, `FilterChipRow`, `EmptyStateCard`)
2. Create separate files for each logical section
3. Use preview functions per component for easier development

**Example Structure**:
```
screens/projects/
├── ProjectsScreen.kt          # Main screen (~100 lines)
├── ProjectsContent.kt         # Content rendering (~150 lines)
├── components/
│   ├── ProjectCard.kt         # Individual project card (~80 lines)
│   ├── ProjectFilterChips.kt  # Filter chips row (~100 lines)
│   └── ProjectEmptyState.kt   # Empty state display (~60 lines)
```

---

### Exception Handling in UI Layer

**Pattern**: Wrap resource loading and async operations in try-catch.

```kotlin
@Composable
fun ProjectDetailScreen(viewModel: ProjectDetailViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Wrap resource loading
    val imageResult = remember(uiState.imageUrl) {
        runCatching {
            // Resource loading that might fail
        }
    }

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorState(
            message = uiState.error,
            onRetry = viewModel::retry
        )
        else -> ProjectDetailContent(
            project = uiState.project,
            imageResult = imageResult
        )
    }
}
```

**Fragile Areas to Watch**:
- Image loading without error handling
- Resource file access
- Platform-specific code in Composables
- Deep linking URL parsing

---

### Positive Patterns Checklist

Validated practices from production KMP/CMP projects:

✓ **Clean codebase** - No TODO/FIXME/HACK comments in production code
✓ **Good test coverage** - Tests for ViewModels, repositories, and domain logic
✓ **Result-based error handling** - Sealed classes for Success/Error/Loading states
✓ **Strong DI setup** - Koin properly configured across all layers
✓ **Security headers configured** - HSTS, X-Frame-Options, CSP in backend
✓ **Database connection pooling** - HikariCP with proper timeouts
✓ **Code quality enforcement** - Detekt with comprehensive rules
✓ **String resources used** - No hardcoded user-facing strings
✓ **Accessibility support** - Content descriptions on interactive elements
✓ **Platform abstractions** - Logger, Analytics, CrashLogger interfaces

---

*Concerns audit: 2026-01-15*
*Update as issues are fixed or new ones discovered*
