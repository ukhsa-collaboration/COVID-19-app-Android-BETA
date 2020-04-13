/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import androidx.room.Room
import com.example.colocate.ble.BleEventTracker
import com.example.colocate.ble.BleEvents
import com.example.colocate.ble.DefaultSaveContactWorker
import com.example.colocate.ble.SaveContactWorker
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2Dao
import com.example.colocate.persistence.SonarIdProvider
import com.example.colocate.persistence.SharedPreferencesSonarIdProvider
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton

@Module
class PersistenceModule(
    private val applicationContext: Context
) {

    @Provides
    fun provideDatabase() =
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "event-database"
        ).fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactEventDao(database: AppDatabase): ContactEventDao =
        database.contactEventDao()

    @Provides
    fun provideContactEventV2Dao(database: AppDatabase): ContactEventV2Dao =
        database.contactEventV2Dao()

    @Provides
    @Singleton
    fun providesBleEvents(): BleEvents = BleEventTracker()

    @Provides
    fun provideSonarIdProvider(): SonarIdProvider =
        SharedPreferencesSonarIdProvider(applicationContext)

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        DefaultSaveContactWorker(ioDispatcher, contactEventDao, contactEventV2Dao)
}
