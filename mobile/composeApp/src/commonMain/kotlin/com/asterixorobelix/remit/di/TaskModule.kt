package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.search.TaskSearchRepository
import com.asterixorobelix.remit.data.task.TaskLocalDataSource
import com.asterixorobelix.remit.data.task.TaskRemoteDataSource
import com.asterixorobelix.remit.data.task.TaskRepositoryImpl
import com.asterixorobelix.remit.domain.search.SearchRepository
import com.asterixorobelix.remit.domain.session.UserDataCleaner
import com.asterixorobelix.remit.domain.task.CreateTaskUseCase
import com.asterixorobelix.remit.domain.task.DeleteTaskUseCase
import com.asterixorobelix.remit.domain.task.ObserveRecentTasksUseCase
import com.asterixorobelix.remit.domain.task.ObserveSyncStatusUseCase
import com.asterixorobelix.remit.domain.task.ObserveTasksUseCase
import com.asterixorobelix.remit.domain.task.SyncTasksUseCase
import com.asterixorobelix.remit.domain.task.TaskRepository
import com.asterixorobelix.remit.domain.task.TaskSyncSettings
import com.asterixorobelix.remit.domain.task.ToggleTaskUseCase
import com.asterixorobelix.remit.presentation.tasks.TasksViewModel
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
