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
import uk.nhs.nhsx.sonar.android.app.ble.BleEventTracker
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.DefaultSaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.contactevents.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.onboarding.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.onboarding.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.StatusStorage
import javax.inject.Named
import javax.inject.Singleton

@Module
class PersistenceModule(
    private val applicationContext: Context
) {

    @Provides
    fun provideDatabase() =
        Room
            .databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "event-database"
            )
            .fallbackToDestructiveMigration()
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
        SonarIdProvider(applicationContext)

    @Provides
    fun provideSaveContactWorker(
        contactEventDao: ContactEventDao,
        contactEventV2Dao: ContactEventV2Dao,
        @Named(AppModule.DISPATCHER_IO) ioDispatcher: CoroutineDispatcher
    ): SaveContactWorker =
        DefaultSaveContactWorker(ioDispatcher, contactEventDao, contactEventV2Dao)

    @Provides
    @Singleton
    fun providePostCodeProvider(): PostCodeProvider =
        PostCodeProvider(applicationContext)

    @Provides
    @Singleton
    fun provideOnboardingStatusProvider(): OnboardingStatusProvider =
        OnboardingStatusProvider(applicationContext)

    @Provides
    fun providesStatusStorage(): StatusStorage =
        StatusStorage(applicationContext)
}
