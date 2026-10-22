package com.example.myproject.di

import com.example.myproject.data.search.TaskSearchRepository
import com.example.myproject.data.task.TaskLocalDataSource
import com.example.myproject.data.task.TaskRemoteDataSource
import com.example.myproject.data.task.TaskRepositoryImpl
import com.example.myproject.domain.search.SearchRepository
import com.example.myproject.domain.session.UserDataCleaner
import com.example.myproject.domain.task.CreateTaskUseCase
import com.example.myproject.domain.task.DeleteTaskUseCase
import com.example.myproject.domain.task.ObserveRecentTasksUseCase
import com.example.myproject.domain.task.ObserveSyncStatusUseCase
import com.example.myproject.domain.task.ObserveTasksUseCase
import com.example.myproject.domain.task.SyncTasksUseCase
import com.example.myproject.domain.task.TaskRepository
import com.example.myproject.domain.task.TaskSyncSettings
import com.example.myproject.domain.task.ToggleTaskUseCase
import com.example.myproject.presentation.tasks.TasksViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

/**
 * Offline-first Tasks. [syncEnabled] is true only when the app has both `backend` and `auth`
 * (the sync endpoint is authenticated); otherwise Tasks are local-only and nothing is queued.
 */
fun taskModule(syncEnabled: Boolean): Module = module {
    single { TaskSyncSettings(syncEnabled) }
    singleOf(::TaskLocalDataSource)
    singleOf(::TaskRemoteDataSource)
    singleOf(::TaskRepositoryImpl) binds arrayOf(TaskRepository::class, UserDataCleaner::class)
    singleOf(::TaskSearchRepository) bind SearchRepository::class
    factoryOf(::ObserveTasksUseCase)
    factoryOf(::ObserveRecentTasksUseCase)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::CreateTaskUseCase)
    factoryOf(::ToggleTaskUseCase)
    factoryOf(::DeleteTaskUseCase)
    factoryOf(::SyncTasksUseCase)
    viewModelOf(::TasksViewModel)
}
