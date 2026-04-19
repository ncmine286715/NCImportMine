package com.ncmine.importmine.di

import android.content.Context
import com.ncmine.importmine.data.repository.AddonRepositoryImpl
import com.ncmine.importmine.domain.repository.AddonRepository
import com.ncmine.importmine.util.BackupManager
import com.ncmine.importmine.util.FileScanner
import com.ncmine.importmine.util.PackConverter
import com.ncmine.importmine.util.PreferenceManager
import com.ncmine.importmine.util.AdMobManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFileScanner(@ApplicationContext context: Context): FileScanner {
        return FileScanner(context)
    }

    @Provides
    @Singleton
    fun providePackConverter(@ApplicationContext context: Context): PackConverter {
        return PackConverter(context)
    }

    @Provides
    @Singleton
    fun provideBackupManager(@ApplicationContext context: Context): BackupManager {
        return BackupManager(context)
    }

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager {
        return PreferenceManager(context)
    }

    @Provides
    @Singleton
    fun provideAdMobManager(@ApplicationContext context: Context): AdMobManager {
        return AdMobManager(context)
    }

    @Provides
    @Singleton
    fun provideAddonRepository(
        @ApplicationContext context: Context,
        fileScanner: FileScanner,
        packConverter: PackConverter,
        backupManager: BackupManager,
        preferenceManager: PreferenceManager
    ): AddonRepository {
        return AddonRepositoryImpl(context, fileScanner, packConverter, backupManager, preferenceManager)
    }
}
