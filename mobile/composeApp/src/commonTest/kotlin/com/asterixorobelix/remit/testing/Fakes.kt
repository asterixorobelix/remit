package com.asterixorobelix.remit.testing

import com.asterixorobelix.remit.domain.auth.AuthRepository
import com.asterixorobelix.remit.domain.auth.AuthUser
import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.domain.common.DataException
import com.asterixorobelix.remit.domain.session.ProfileCache
import com.asterixorobelix.remit.domain.session.SessionRepository
import com.asterixorobelix.remit.domain.session.UserDataCleaner
import com.asterixorobelix.remit.domain.session.UserProfile
import com.asterixorobelix.remit.domain.settings.AppSettingsRepository
import com.asterixorobelix.remit.domain.settings.ThemeMode
import com.asterixorobelix.remit.domain.task.SyncStatus
import com.asterixorobelix.remit.domain.task.Task
import com.asterixorobelix.remit.domain.task.TaskRepository
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.AppDispatchers
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.platform.Environment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class FakeAppConfig(
    override val capabilities: Set<Capability> = setOf(Capability.AUTH, Capability.OFFLINE, Capability.BACKEND),
    override val environment: Environment = Environment.DEVELOPMENT,
    override val apiBaseUrl: String = "https://api.test",
    override val isDebug: Boolean = true,
    override val versionName: String = "1.2.0",
    override val versionCode: Int = 10200,
    override val applicationId: String = "com.asterixorobelix.remit",
) : AppConfig

class TestAppDispatchers(override val io: CoroutineDispatcher) : AppDispatchers

class FakeAuthRepository : AuthRepository {
    var lastEmail: String? = null
    var lastDisplayName: String? = null
    var failure: DataError? = null
    var logoutCalls = 0
    var deleteCalls = 0
    var profile = UserProfile("Test User", "test@example.com")

    override suspend fun login(email: String, password: String): Result<AuthUser> {
        lastEmail = email
        return result { AuthUser(1, email, "Test") }
    }

    override suspend fun register(displayName: String, email: String, password: String): Result<AuthUser> {
        lastEmail = email
        lastDisplayName = displayName
        return result { AuthUser(1, email, displayName) }
    }

    override suspend fun logout() {
        logoutCalls++
    }

    override suspend fun deleteAccount(): Result<Unit> {
        deleteCalls++
        return result { }
    }

    override suspend fun refreshProfile(): Result<UserProfile> = result { profile }

    override suspend fun updateDisplayName(displayName: String): Result<UserProfile> {
        lastDisplayName = displayName
        return result { profile.copy(displayName = displayName).also { profile = it } }
    }

    private fun <T> result(value: () -> T): Result<T> =
        failure?.let { Result.failure(DataException(it)) } ?: Result.success(value())
}

class FakeProfileCache(var profile: UserProfile? = null) : ProfileCache {
    override fun cachedProfile(): UserProfile? = profile
    override fun save(profile: UserProfile) {
        this.profile = profile
    }
    override fun clear() {
        profile = null
    }
}

class FakeSessionRepository(var signedIn: Boolean = false) : SessionRepository {
    val expiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var clearCalls = 0
    override suspend fun hasSession(): Boolean = signedIn
    override val sessionExpired: Flow<Unit> = expiredEvents
    override suspend fun clearLocalSession() {
        clearCalls++
        signedIn = false
    }
}

class RecordingCleaner : UserDataCleaner {
    var calls = 0
    override suspend fun clearUserData() {
        calls++
    }
}

class FakeAppSettingsRepository(
    private var onboardingDone: Boolean = true,
    private var opens: Int = 0,
    private var reviewRequested: Boolean = false,
) : AppSettingsRepository {
    private val mutableTheme = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = mutableTheme.asStateFlow()
    override fun setThemeMode(mode: ThemeMode) {
        mutableTheme.value = mode
    }
    override fun hasCompletedOnboarding(): Boolean = onboardingDone
    override fun completeOnboarding() {
        onboardingDone = true
    }
    override fun recordAppOpen(): Int = ++opens
    override fun hasRequestedReview(): Boolean = reviewRequested
    override fun markReviewRequested() {
        reviewRequested = true
    }
}

class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    var syncFailure: DataError? = null
    var observeFailure: Throwable? = null
    val syncCalls = mutableListOf<Boolean>()
    val created = mutableListOf<String>()
    val toggled = mutableListOf<Task>()
    val deleted = mutableListOf<Task>()

    override fun observeTasks(): Flow<List<Task>> = tasks.map { list ->
        observeFailure?.let { throw it }
        list
    }
    override fun observeRecentTasks(limit: Int): Flow<List<Task>> = tasks.map { it.take(limit) }
    override suspend fun search(query: String, limit: Int): List<Task> =
        tasks.value.filter { it.title.contains(query, ignoreCase = true) }.take(limit)
    override val syncStatus: StateFlow<SyncStatus> = status.asStateFlow()

    override suspend fun create(title: String) {
        created += title
        tasks.value = tasks.value + Task("id-${created.size}", title, completed = false, updatedAt = 1, createdAt = 1)
    }

    override suspend fun toggle(task: Task) {
        toggled += task
    }

    override suspend fun delete(task: Task) {
        deleted += task
    }

    override suspend fun sync(force: Boolean): Result<Unit> {
        syncCalls += force
        return syncFailure?.let { Result.failure(DataException(it)) } ?: Result.success(Unit)
    }
}
