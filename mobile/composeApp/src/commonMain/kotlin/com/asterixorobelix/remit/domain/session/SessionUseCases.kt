package com.asterixorobelix.remit.domain.session

class GetCachedProfileUseCase(private val cache: ProfileCache) {
    operator fun invoke(): UserProfile? = cache.cachedProfile()
}

class ObserveSessionExpiredUseCase(private val repository: SessionRepository) {
    operator fun invoke() = repository.sessionExpired
}

/** Up to two initials from the display name, falling back to the first letter of the email. */
class ComputeInitialsUseCase {
    operator fun invoke(profile: UserProfile): String {
        val fromName = profile.displayName
            .split(' ')
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .take(MAX_INITIALS)
            .joinToString("")
        return fromName.ifEmpty { profile.email.firstOrNull()?.uppercaseChar()?.toString().orEmpty() }
    }

    private companion object {
        const val MAX_INITIALS = 2
    }
}
