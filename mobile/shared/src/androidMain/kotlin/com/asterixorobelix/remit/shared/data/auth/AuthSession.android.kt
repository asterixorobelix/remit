package com.asterixorobelix.remit.shared.data.auth

import com.asterixorobelix.remit.shared.data.FirebaseSettings
import com.asterixorobelix.remit.shared.data.defaultFirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

actual fun platformAuthSession(settings: FirebaseSettings): AuthSession {
    val app = defaultFirebaseApp() ?: return UnsupportedPlatformAuthSession
    val auth = FirebaseAuth.getInstance(app)
    settings.emulatorHost?.let { auth.useEmulator(it, FirebaseSettings.AUTH_EMULATOR_PORT) }
    return FirebaseAuthSession(auth)
}

/** Anonymous Firebase Auth; Firebase persists the user, so the id survives app restarts. */
internal class FirebaseAuthSession(private val auth: FirebaseAuth) : AuthSession {
    private val signIn = Mutex()

    override suspend fun userId(): String = currentUser().uid

    override suspend fun idToken(forceRefresh: Boolean): String {
        val user = currentUser()
        return firebaseCall { user.getIdToken(forceRefresh).await().token }
            ?: throw AuthUnavailableException(isNetworkError = false)
    }

    /** One sign-in at a time, so concurrent callers do not create two anonymous users. */
    private suspend fun currentUser(): FirebaseUser = signIn.withLock {
        auth.currentUser
            ?: firebaseCall { auth.signInAnonymously().await().user }
            ?: throw AuthUnavailableException(isNetworkError = false)
    }

    private suspend fun <T> firebaseCall(block: suspend () -> T): T = try {
        block()
    } catch (error: FirebaseNetworkException) {
        throw AuthUnavailableException(isNetworkError = true, cause = error)
    } catch (error: FirebaseException) {
        throw AuthUnavailableException(isNetworkError = false, cause = error)
    }
}
