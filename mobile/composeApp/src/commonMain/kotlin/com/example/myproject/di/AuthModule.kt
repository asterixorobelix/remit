package com.example.myproject.di

import com.example.myproject.data.auth.AuthApi
import com.example.myproject.data.auth.AuthRepositoryImpl
import com.example.myproject.domain.auth.AuthRepository
import com.example.myproject.domain.auth.DeleteAccountUseCase
import com.example.myproject.domain.auth.LoginUseCase
import com.example.myproject.domain.auth.LogoutUseCase
import com.example.myproject.domain.auth.RefreshProfileUseCase
import com.example.myproject.domain.auth.RegisterUseCase
import com.example.myproject.domain.auth.UpdateDisplayNameUseCase
import com.example.myproject.domain.auth.ValidateLoginUseCase
import com.example.myproject.domain.auth.ValidateRegistrationUseCase
import com.example.myproject.presentation.auth.LoginViewModel
import com.example.myproject.presentation.auth.RegisterViewModel
import com.example.myproject.presentation.profile.ProfileViewModel
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
