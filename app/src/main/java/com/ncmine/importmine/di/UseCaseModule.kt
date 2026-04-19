package com.ncmine.importmine.di

import com.ncmine.importmine.domain.repository.AddonRepository
import com.ncmine.importmine.domain.usecase.ImportAddonUseCase
import com.ncmine.importmine.domain.usecase.ScanAddonsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideScanAddonsUseCase(repository: AddonRepository): ScanAddonsUseCase {
        return ScanAddonsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideImportAddonUseCase(repository: AddonRepository): ImportAddonUseCase {
        return ImportAddonUseCase(repository)
    }
}
