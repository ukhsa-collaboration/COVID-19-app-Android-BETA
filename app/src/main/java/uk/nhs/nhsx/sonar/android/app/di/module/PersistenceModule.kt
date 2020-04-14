/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import uk.nhs.nhsx.sonar.android.app.ble.BleEventTracker
import uk.nhs.nhsx.sonar.android.app.ble.BleEvents
import uk.nhs.nhsx.sonar.android.app.ble.DefaultSaveContactWorker
import uk.nhs.nhsx.sonar.android.app.ble.SaveContactWorker
import uk.nhs.nhsx.sonar.android.app.persistence.AppDatabase
import uk.nhs.nhsx.sonar.android.app.persistence.ContactEventDao
import uk.nhs.nhsx.sonar.android.app.persistence.ContactEventV2Dao
import uk.nhs.nhsx.sonar.android.app.persistence.OnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.persistence.PostCodeProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SharedPreferencesOnboardingStatusProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SharedPreferencesPostCodeProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SharedPreferencesSonarIdProvider
import uk.nhs.nhsx.sonar.android.app.persistence.SonarIdProvider
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

    @Provides
    @Singleton
    fun providePostCodeProvider(): PostCodeProvider =
        SharedPreferencesPostCodeProvider(applicationContext)

    @Provides
    @Singleton
    fun provideOnboardingStatusProvider(): OnboardingStatusProvider =
        SharedPreferencesOnboardingStatusProvider(applicationContext)
}
