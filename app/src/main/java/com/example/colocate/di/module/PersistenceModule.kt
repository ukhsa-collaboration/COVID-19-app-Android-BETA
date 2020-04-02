/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import androidx.room.Room
import com.example.colocate.ble.LongLiveConnectionScan
import com.example.colocate.ble.SaveContactWorker
import com.example.colocate.ble.Scan
import com.example.colocate.ble.Scanner
import com.example.colocate.persistence.AppDatabase
import com.example.colocate.persistence.ContactEventDao
import com.example.colocate.persistence.ContactEventV2Dao
import com.example.colocate.persistence.ResidentIdProvider
import com.example.colocate.persistence.SharedPreferencesResidentIdProvider
import com.polidea.rxandroidble2.RxBleClient
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named

@Module
class PersistenceModule(
    private val applicationContext: Context,
    private val connectionV2: Boolean
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
    fun provideContactEventV2Dao(database: AppDatabase): ContactEventV2Dao {
        return database.contactEventV2Dao()
    }

    @Provides
    fun provideResidentIdProvider(): ResidentIdProvider =
        SharedPreferencesResidentIdProvider(applicationContext)

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        SaveContactWorker(ioDispatcher, contactEventDao, contactEventV2Dao)

    @Provides
    fun provideScanner(
        rxBleClient: RxBleClient,
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        saveContactWorker: SaveContactWorker,
        @Named(AppModule.DISPATCHER_IO) dispatcher: CoroutineDispatcher
    ): Scanner {
        return if (connectionV2) {
            LongLiveConnectionScan(rxBleClient, contactEventDao, contactEventV2Dao, dispatcher)
        } else {
            Scan(rxBleClient, contactEventDao, contactEventV2Dao, saveContactWorker, dispatcher)
        }
    }

    @Provides
    @Named(USE_CONNECTION_V2)
    fun provideUseConnectionV2() = connectionV2

    companion object {
        const val USE_CONNECTION_V2 = "USE_CONNECTION_V2"
    }
}
