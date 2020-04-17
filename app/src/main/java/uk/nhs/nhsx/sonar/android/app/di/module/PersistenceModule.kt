/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import uk.nhs.nhsx.sonar.android.app.AppDatabase
import uk.nhs.nhsx.sonar.android.app.ble.DefaultSaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.notifications.AcknowledgementsDao
import javax.inject.Named

@Module
class PersistenceModule(private val appContext: Context) {

    @Provides
    fun provideDatabase() =
        Room
            .databaseBuilder(appContext, AppDatabase::class.java, "event-database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContactEventV2Dao(database: AppDatabase): ContactEventDao =
        database.contactEventDao()

    @Provides
    fun provideAcknowledgementsDao(database: AppDatabase): AcknowledgementsDao =
        database.acknowledgementsDao()

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        @Named(BluetoothModule.ERROR_MARGIN) errorMargin: Int,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        DefaultSaveContactWorker(ioDispatcher, errorMargin, contactEventDao)
}
