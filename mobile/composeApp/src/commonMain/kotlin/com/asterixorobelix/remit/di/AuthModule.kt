package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.auth.AuthApi
import com.asterixorobelix.remit.data.auth.AuthRepositoryImpl
import com.asterixorobelix.remit.domain.auth.AuthRepository
import com.asterixorobelix.remit.domain.auth.DeleteAccountUseCase
import com.asterixorobelix.remit.domain.auth.LoginUseCase
import com.asterixorobelix.remit.domain.auth.LogoutUseCase
import com.asterixorobelix.remit.domain.auth.RefreshProfileUseCase
import com.asterixorobelix.remit.domain.auth.RegisterUseCase
import com.asterixorobelix.remit.domain.auth.UpdateDisplayNameUseCase
import com.asterixorobelix.remit.domain.auth.ValidateLoginUseCase
import com.asterixorobelix.remit.domain.auth.ValidateRegistrationUseCase
import com.asterixorobelix.remit.presentation.auth.LoginViewModel
import com.asterixorobelix.remit.presentation.auth.RegisterViewModel
import com.asterixorobelix.remit.presentation.profile.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authModule = module {
    singleOf(::AuthApi)
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    factoryOf(::ValidateLoginUseCase)
    factoryOf(::ValidateRegistrationUseCase)
    factoryOf(::LoginUseCase)
    factoryOf(::RegisterUseCase)
    factoryOf(::RefreshProfileUseCase)
    factoryOf(::UpdateDisplayNameUseCase)
    // Sign-out and deletion wipe every feature's per-user data (getAll is not constructor-injectable).
    factory { LogoutUseCase(get(), getAll()) }
    factory { DeleteAccountUseCase(get(), getAll()) }
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ProfileViewModel)
}
